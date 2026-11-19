package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ProfileAssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignProfileUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileAssignmentCreationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerProfileAssignmentCreationUseCase;
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
 * Implementación de {@link AdministerProfileAssignmentCreationUseCase} (HU-019). Retrofit HU-021:
 * audita {@code PROFILE_ASSIGNED} en ambas ramas sin cambiar el resultado.
 */
public final class AdministerProfileAssignmentCreationUseCaseImpl implements AdministerProfileAssignmentCreationUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final AssignProfileUseCase assignProfile;
    private final AdministrationAuditRepository audit;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public AdministerProfileAssignmentCreationUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
                                                          AssignProfileUseCase assignProfile, AdministrationAuditRepository audit, IdentifierGenerator identifiers,
                                                          TimeProvider time) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.assignProfile = Objects.requireNonNull(assignProfile, RequiredArgumentMessages.ASSIGN_PROFILE_USE_CASE);
        this.audit = Objects.requireNonNull(audit, RequiredArgumentMessages.ADMINISTRATION_AUDIT_REPOSITORY);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<ProfileAssignmentResponse> execute(AdministerProfileAssignmentCreationRequest input) {
        return mustBeAdministrator.execute(input.administration())
                .then(Mono.defer(() -> assignProfile.execute(input.assignment())))
                .flatMap(response -> recordAudit(input.administration(), AdministrationOutcome.ALLOWED).thenReturn(response))
                .onErrorResume(NotAuthorizedToAdministerException.class,
                        error -> recordAudit(input.administration(), AdministrationOutcome.DENIED).then(Mono.error(error)));
    }

    private Mono<Void> recordAudit(AdministrationRequest context, AdministrationOutcome outcome) {
        UUID eventId = identifiers.next();
        AdministrationEvent event = new AdministrationEvent(eventId, eventId.toString(), context.tenantId(),
                context.applicationId(), context.subject(), AdministrationOperation.PROFILE_ASSIGNED, outcome, time.now());
        return Mono.defer(() -> audit.save(event)).onErrorResume(error -> Mono.empty());
    }
}
