package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileRoleAdditionRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerProfileRoleAdditionUseCase;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
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
 * Implementación de {@link AdministerProfileRoleAdditionUseCase} (HU-019). Retrofit HU-021:
 * audita {@code PROFILE_ROLE_ADDED} en ambas ramas del camino gateado — sin
 * {@code administration} (perfil TENANT) no hay gate ni auditoría.
 */
public final class AdministerProfileRoleAdditionUseCaseImpl implements AdministerProfileRoleAdditionUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final AddRoleToProfileUseCase addRoleToProfile;
    private final RemoveRoleFromProfileUseCase removeRoleFromProfile;
    private final AdministrationAuditRepository audit;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public AdministerProfileRoleAdditionUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
                                                    AddRoleToProfileUseCase addRoleToProfile, RemoveRoleFromProfileUseCase removeRoleFromProfile, AdministrationAuditRepository audit,
                                                    IdentifierGenerator identifiers, TimeProvider time) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
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
                                                    AddRoleToProfileUseCase addRoleToProfile, AdministrationAuditRepository audit,
                                                    IdentifierGenerator identifiers, TimeProvider time) {
        this(mustBeAdministrator, addRoleToProfile,
                input -> Mono.error(new IllegalStateException("La revocación de roles del perfil no está configurada")),
                audit, identifiers, time);
    }

    @Override
    public Mono<ProfileResponse> execute(AdministerProfileRoleAdditionRequest input) {
        Mono<Void> gate = input.administration().map(mustBeAdministrator::execute).orElseGet(Mono::empty);
        Mono<ProfileResponse> result = gate.then(Mono.defer(() -> input.removal()
                ? removeRoleFromProfile.execute(new RemoveRoleFromProfileRequest(input.addition().tenantId(), input.addition().profileId(), input.addition().roleId()))
                : addRoleToProfile.execute(input.addition())));
        AdministrationOperation operation = input.removal() ? AdministrationOperation.PROFILE_ROLE_REMOVED
                : AdministrationOperation.PROFILE_ROLE_ADDED;
        return input.administration().map(administration -> audited(result, administration, operation)).orElse(result);
    }

    private Mono<ProfileResponse> audited(Mono<ProfileResponse> result, AdministrationRequest administration,
                                          AdministrationOperation operation) {
        return result
                .flatMap(response -> recordAudit(administration, operation, AdministrationOutcome.ALLOWED).thenReturn(response))
                .onErrorResume(NotAuthorizedToAdministerException.class,
                        error -> recordAudit(administration, operation, AdministrationOutcome.DENIED).then(Mono.error(error)));
    }

    private Mono<Void> recordAudit(AdministrationRequest context, AdministrationOperation operation, AdministrationOutcome outcome) {
        UUID eventId = identifiers.next();
        AdministrationEvent event = new AdministrationEvent(eventId, eventId.toString(), context.tenantId(),
                context.applicationId(), context.subject(), operation, outcome, time.now());
        return Mono.defer(() -> audit.save(event)).onErrorResume(error -> Mono.empty());
    }
}
