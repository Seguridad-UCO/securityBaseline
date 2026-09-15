package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileDefinitionRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerProfileDefinitionUseCase;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
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
 * Implementación de {@link AdministerProfileDefinitionUseCase} (HU-019). Retrofit HU-021: audita
 * {@code PROFILE_DEFINED} en ambas ramas del camino gateado — sin {@code administration} (perfil
 * TENANT) no hay gate ni auditoría.
 */
public final class AdministerProfileDefinitionUseCaseImpl implements AdministerProfileDefinitionUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final DefineProfileUseCase defineProfile;
    private final AdministrationAuditRepository audit;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public AdministerProfileDefinitionUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
            DefineProfileUseCase defineProfile, AdministrationAuditRepository audit, IdentifierGenerator identifiers,
            TimeProvider time) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.defineProfile = Objects.requireNonNull(defineProfile, RequiredArgumentMessages.DEFINE_PROFILE_USE_CASE);
        this.audit = Objects.requireNonNull(audit, RequiredArgumentMessages.ADMINISTRATION_AUDIT_REPOSITORY);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<ProfileResponse> execute(AdministerProfileDefinitionRequest input) {
        Mono<Void> gate = input.administration().map(mustBeAdministrator::execute).orElseGet(Mono::empty);
        Mono<ProfileResponse> result = gate.then(Mono.defer(() -> defineProfile.execute(input.profile())));
        return input.administration().map(administration -> audited(result, administration)).orElse(result);
    }

    private Mono<ProfileResponse> audited(Mono<ProfileResponse> result, AdministrationRequest administration) {
        return result
                .flatMap(response -> recordAudit(administration, AdministrationOutcome.ALLOWED).thenReturn(response))
                .onErrorResume(NotAuthorizedToAdministerException.class,
                        error -> recordAudit(administration, AdministrationOutcome.DENIED).then(Mono.error(error)));
    }

    private Mono<Void> recordAudit(AdministrationRequest context, AdministrationOutcome outcome) {
        UUID eventId = identifiers.next();
        AdministrationEvent event = new AdministrationEvent(eventId, eventId.toString(), context.tenantId(),
                context.applicationId(), context.subject(), AdministrationOperation.PROFILE_DEFINED, outcome, time.now());
        return Mono.defer(() -> audit.save(event)).onErrorResume(error -> Mono.empty());
    }
}
