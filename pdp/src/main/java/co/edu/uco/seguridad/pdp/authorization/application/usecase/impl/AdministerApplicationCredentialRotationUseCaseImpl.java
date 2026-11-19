package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.RotateApplicationCredentialRequest;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.ApplicationRegistrationResponse;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RotateApplicationCredentialUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerApplicationCredentialRotationUseCase;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
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
 * Implementación de {@link AdministerApplicationCredentialRotationUseCase} (HU-015). Retrofit
 * HU-021: audita {@code APPLICATION_CREDENTIAL_ROTATED} en ambas ramas sin cambiar el resultado.
 */
public final class AdministerApplicationCredentialRotationUseCaseImpl
        implements AdministerApplicationCredentialRotationUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final RotateApplicationCredentialUseCase rotateCredential;
    private final AdministrationAuditRepository audit;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public AdministerApplicationCredentialRotationUseCaseImpl(
            PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
            RotateApplicationCredentialUseCase rotateCredential, AdministrationAuditRepository audit,
            IdentifierGenerator identifiers, TimeProvider time) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.rotateCredential = Objects.requireNonNull(rotateCredential,
                RequiredArgumentMessages.ROTATE_APPLICATION_CREDENTIAL_USE_CASE);
        this.audit = Objects.requireNonNull(audit, RequiredArgumentMessages.ADMINISTRATION_AUDIT_REPOSITORY);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<ApplicationRegistrationResponse> execute(AdministrationRequest input) {
        return mustBeAdministrator.execute(input)
                .then(Mono.defer(() -> rotateCredential.execute(
                        new RotateApplicationCredentialRequest(input.tenantId(), input.applicationId()))))
                .flatMap(response -> recordAudit(input, AdministrationOutcome.ALLOWED).thenReturn(response))
                .onErrorResume(NotAuthorizedToAdministerException.class,
                        error -> recordAudit(input, AdministrationOutcome.DENIED).then(Mono.error(error)));
    }

    private Mono<Void> recordAudit(AdministrationRequest context, AdministrationOutcome outcome) {
        UUID eventId = identifiers.next();
        AdministrationEvent event = new AdministrationEvent(eventId, eventId.toString(), context.tenantId(),
                context.applicationId(), context.subject(), AdministrationOperation.APPLICATION_CREDENTIAL_ROTATED,
                outcome, time.now());
        return Mono.defer(() -> audit.save(event)).onErrorResume(error -> Mono.empty());
    }
}
