package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileRoleAdditionRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.CatalogAdministration;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.TenantAdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustAdministerAtLeastOneApplicationValidator;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerProfileRoleAdditionUseCase;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerTenantCatalogException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.RemoveRoleFromProfileRequest;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.AddRoleToProfileUseCase;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.RemoveRoleFromProfileUseCase;
import co.edu.uco.seguridad.shared.audit.AdministrationAuditRepository;
import co.edu.uco.seguridad.shared.audit.AdministrationEvent;
import co.edu.uco.seguridad.shared.audit.AdministrationOperation;
import co.edu.uco.seguridad.shared.audit.AdministrationOutcome;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

/**
 * Implementación de {@link AdministerProfileRoleAdditionUseCase} (HU-019, HU-036). Agregar o quitar un rol
 * de un perfil edita el perfil: si es {@code APPLICATION} se exige administrar esa aplicación; si es
 * {@code TENANT}, administrar al menos una aplicación del inquilino (ADR-036). Retrofit HU-021: audita
 * {@code PROFILE_ROLE_ADDED} / {@code PROFILE_ROLE_REMOVED}.
 */
public final class AdministerProfileRoleAdditionUseCaseImpl implements AdministerProfileRoleAdditionUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final PrincipalMustAdministerAtLeastOneApplicationValidator mustAdministerSomeApplication;
    private final AddRoleToProfileUseCase addRoleToProfile;
    private final RemoveRoleFromProfileUseCase removeRoleFromProfile;
    private final AdministrationAuditRepository audit;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public AdministerProfileRoleAdditionUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
                                                    PrincipalMustAdministerAtLeastOneApplicationValidator mustAdministerSomeApplication,
                                                    AddRoleToProfileUseCase addRoleToProfile, RemoveRoleFromProfileUseCase removeRoleFromProfile, AdministrationAuditRepository audit,
                                                    IdentifierGenerator identifiers, TimeProvider time) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.mustAdministerSomeApplication = Objects.requireNonNull(mustAdministerSomeApplication,
                RequiredArgumentMessages.PRINCIPAL_MUST_ADMINISTER_AT_LEAST_ONE_APPLICATION_VALIDATOR);
        this.addRoleToProfile = Objects.requireNonNull(addRoleToProfile, RequiredArgumentMessages.ADD_ROLE_TO_PROFILE_USE_CASE);
        this.removeRoleFromProfile = Objects.requireNonNull(removeRoleFromProfile, "removeRoleFromProfile");
        this.audit = Objects.requireNonNull(audit, RequiredArgumentMessages.ADMINISTRATION_AUDIT_REPOSITORY);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    /**
     * Conserva el contrato de construcción de la operación de alta. La baja solo se habilita
     * desde el adaptador que aporta explícitamente su caso de uso.
     */
    public AdministerProfileRoleAdditionUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
                                                    PrincipalMustAdministerAtLeastOneApplicationValidator mustAdministerSomeApplication,
                                                    AddRoleToProfileUseCase addRoleToProfile, AdministrationAuditRepository audit,
                                                    IdentifierGenerator identifiers, TimeProvider time) {
        this(mustBeAdministrator, mustAdministerSomeApplication, addRoleToProfile,
                input -> Mono.error(new IllegalStateException("La revocación de roles del perfil no está configurada")),
                audit, identifiers, time);
    }

    @Override
    public Mono<ProfileResponse> execute(AdministerProfileRoleAdditionRequest input) {
        AdministrationOperation operation = input.removal() ? AdministrationOperation.PROFILE_ROLE_REMOVED
                : AdministrationOperation.PROFILE_ROLE_ADDED;
        return switch (input.administration()) {
            case CatalogAdministration.OfApplication application ->
                    applicationScoped(application.administration(), input, operation);
            case CatalogAdministration.OfTenant tenant -> tenantScoped(tenant.actor(), input, operation);
        };
    }

    private Mono<ProfileResponse> applicationScoped(AdministrationRequest administration,
                                                    AdministerProfileRoleAdditionRequest input,
                                                    AdministrationOperation operation) {
        return mustBeAdministrator.execute(administration)
                .then(Mono.defer(() -> addOrRemove(input)))
                .flatMap(response -> recordAudit(administration.tenantId(), administration.applicationId(),
                        administration.subject(), operation, AdministrationOutcome.ALLOWED).thenReturn(response))
                .onErrorResume(NotAuthorizedToAdministerException.class,
                        error -> recordAudit(administration.tenantId(), administration.applicationId(),
                                administration.subject(), operation, AdministrationOutcome.DENIED).then(Mono.error(error)));
    }

    private Mono<ProfileResponse> tenantScoped(TenantAdministrationRequest actor, AdministerProfileRoleAdditionRequest input,
                                               AdministrationOperation operation) {
        return mustAdministerSomeApplication.execute(actor)
                .onErrorResume(NotAuthorizedToAdministerTenantCatalogException.class,
                        error -> auditTenantDenial(actor, operation, error).then(Mono.error(error)))
                .flatMap(grantedBy -> addOrRemove(input)
                        .flatMap(response -> recordAudit(actor.tenantId(), grantedBy, actor.subject(), operation,
                                AdministrationOutcome.ALLOWED).thenReturn(response)));
    }

    private Mono<ProfileResponse> addOrRemove(AdministerProfileRoleAdditionRequest input) {
        return input.removal()
                ? removeRoleFromProfile.execute(new RemoveRoleFromProfileRequest(input.addition().tenantId(), input.addition().profileId(), input.addition().roleId()))
                : addRoleToProfile.execute(input.addition());
    }

    private Mono<Void> auditTenantDenial(TenantAdministrationRequest actor, AdministrationOperation operation,
                                         NotAuthorizedToAdministerTenantCatalogException denial) {
        return denial.evaluatedApplication()
                .map(evaluated -> recordAudit(actor.tenantId(), evaluated, actor.subject(), operation,
                        AdministrationOutcome.DENIED))
                .orElseGet(Mono::empty);
    }

    private Mono<Void> recordAudit(TenantId tenantId, ApplicationId applicationId, String subject,
                                   AdministrationOperation operation, AdministrationOutcome outcome) {
        UUID eventId = identifiers.next();
        AdministrationEvent event = new AdministrationEvent(eventId, eventId.toString(), tenantId, applicationId,
                subject, operation, outcome, time.now());
        return Mono.defer(() -> audit.save(event)).onErrorResume(error -> Mono.empty());
    }
}
