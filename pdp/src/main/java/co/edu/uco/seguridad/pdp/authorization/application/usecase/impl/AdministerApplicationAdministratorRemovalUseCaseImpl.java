package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.usecase.RemoveApplicationAdministratorUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerApplicationAdministratorRemovalRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerApplicationAdministratorRemovalUseCase;
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
 * Retrofit HU-021: audita {@code ADMINISTRATOR_REMOVED} en ambas ramas sin cambiar el resultado.
 */
public final class AdministerApplicationAdministratorRemovalUseCaseImpl
        implements AdministerApplicationAdministratorRemovalUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final RemoveApplicationAdministratorUseCase removeApplicationAdministrator;
    private final AdministrationAuditRepository audit;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public AdministerApplicationAdministratorRemovalUseCaseImpl(
            PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
            RemoveApplicationAdministratorUseCase removeApplicationAdministrator, AdministrationAuditRepository audit,
            IdentifierGenerator identifiers, TimeProvider time) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.removeApplicationAdministrator = Objects.requireNonNull(removeApplicationAdministrator,
                RequiredArgumentMessages.REMOVE_APPLICATION_ADMINISTRATOR_USE_CASE);
        this.audit = Objects.requireNonNull(audit, RequiredArgumentMessages.ADMINISTRATION_AUDIT_REPOSITORY);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<Void> execute(AdministerApplicationAdministratorRemovalRequest input) {
        return mustBeAdministrator.execute(input.administration())
                .then(Mono.defer(() -> removeApplicationAdministrator.execute(input.removal())))
                .then(Mono.defer(() -> recordAudit(input.administration(), AdministrationOutcome.ALLOWED)))
                .onErrorResume(NotAuthorizedToAdministerException.class,
                        error -> recordAudit(input.administration(), AdministrationOutcome.DENIED).then(Mono.error(error)));
    }

    private Mono<Void> recordAudit(AdministrationRequest context, AdministrationOutcome outcome) {
        UUID eventId = identifiers.next();
        AdministrationEvent event = new AdministrationEvent(eventId, eventId.toString(), context.tenantId(),
                context.applicationId(), context.subject(), AdministrationOperation.ADMINISTRATOR_REMOVED, outcome,
                time.now());
        return Mono.defer(() -> audit.save(event)).onErrorResume(error -> Mono.empty());
    }
}
