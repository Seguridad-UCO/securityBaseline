package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileDefinitionRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.CatalogAdministration;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.TenantAdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustAdministerAtLeastOneApplicationValidator;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerProfileDefinitionUseCase;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerTenantCatalogException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.DefineProfileUseCase;
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
 * Implementación de {@link AdministerProfileDefinitionUseCase} (HU-019, HU-036). Mismo criterio que
 * {@code AdministerRoleDefinitionUseCaseImpl}: un perfil {@code APPLICATION} exige administrar esa
 * aplicación; un perfil {@code TENANT} exige administrar al menos una aplicación del inquilino (ADR-036),
 * auditado contra la aplicación que concedió el permiso o, si se denegó, la primera evaluada. Retrofit
 * HU-021: audita {@code PROFILE_DEFINED}.
 */
public final class AdministerProfileDefinitionUseCaseImpl implements AdministerProfileDefinitionUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final PrincipalMustAdministerAtLeastOneApplicationValidator mustAdministerSomeApplication;
    private final DefineProfileUseCase defineProfile;
    private final AdministrationAuditRepository audit;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public AdministerProfileDefinitionUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
                                                  PrincipalMustAdministerAtLeastOneApplicationValidator mustAdministerSomeApplication,
                                                  DefineProfileUseCase defineProfile, AdministrationAuditRepository audit, IdentifierGenerator identifiers,
                                                  TimeProvider time) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.mustAdministerSomeApplication = Objects.requireNonNull(mustAdministerSomeApplication,
                RequiredArgumentMessages.PRINCIPAL_MUST_ADMINISTER_AT_LEAST_ONE_APPLICATION_VALIDATOR);
        this.defineProfile = Objects.requireNonNull(defineProfile, RequiredArgumentMessages.DEFINE_PROFILE_USE_CASE);
        this.audit = Objects.requireNonNull(audit, RequiredArgumentMessages.ADMINISTRATION_AUDIT_REPOSITORY);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<ProfileResponse> execute(AdministerProfileDefinitionRequest input) {
        return switch (input.administration()) {
            case CatalogAdministration.OfApplication application -> applicationScoped(application.administration(), input);
            case CatalogAdministration.OfTenant tenant -> tenantScoped(tenant.actor(), input);
        };
    }

    private Mono<ProfileResponse> applicationScoped(AdministrationRequest administration,
                                                    AdministerProfileDefinitionRequest input) {
        return mustBeAdministrator.execute(administration)
                .then(Mono.defer(() -> defineProfile.execute(input.profile())))
                .flatMap(response -> recordAudit(administration.tenantId(), administration.applicationId(),
                        administration.subject(), AdministrationOutcome.ALLOWED).thenReturn(response))
                .onErrorResume(NotAuthorizedToAdministerException.class,
                        error -> recordAudit(administration.tenantId(), administration.applicationId(),
                                administration.subject(), AdministrationOutcome.DENIED).then(Mono.error(error)));
    }

    private Mono<ProfileResponse> tenantScoped(TenantAdministrationRequest actor, AdministerProfileDefinitionRequest input) {
        return mustAdministerSomeApplication.execute(actor)
                .onErrorResume(NotAuthorizedToAdministerTenantCatalogException.class,
                        error -> auditTenantDenial(actor, error).then(Mono.error(error)))
                .flatMap(grantedBy -> defineProfile.execute(input.profile())
                        .flatMap(response -> recordAudit(actor.tenantId(), grantedBy, actor.subject(),
                                AdministrationOutcome.ALLOWED).thenReturn(response)));
    }

    private Mono<Void> auditTenantDenial(TenantAdministrationRequest actor, NotAuthorizedToAdministerTenantCatalogException denial) {
        return denial.evaluatedApplication()
                .map(evaluated -> recordAudit(actor.tenantId(), evaluated, actor.subject(), AdministrationOutcome.DENIED))
                .orElseGet(Mono::empty);
    }

    private Mono<Void> recordAudit(TenantId tenantId, ApplicationId applicationId, String subject,
                                   AdministrationOutcome outcome) {
        UUID eventId = identifiers.next();
        AdministrationEvent event = new AdministrationEvent(eventId, eventId.toString(), tenantId, applicationId,
                subject, AdministrationOperation.PROFILE_DEFINED, outcome, time.now());
        return Mono.defer(() -> audit.save(event)).onErrorResume(error -> Mono.empty());
    }
}
