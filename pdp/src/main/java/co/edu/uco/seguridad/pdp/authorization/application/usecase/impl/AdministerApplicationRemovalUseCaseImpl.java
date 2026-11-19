package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.usecase.RemoveApplicationUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerApplicationRemovalUseCase;
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
 * Implementación de {@link AdministerApplicationRemovalUseCase} (HU-015). Retrofit HU-021: audita
 * {@code APPLICATION_REMOVED} en ambas ramas sin cambiar el resultado — un fallo al auditar nunca
 * bloquea la respuesta (mismo criterio que {@code AuthorizeUseCaseImpl.recordAudit}).
 */
public final class AdministerApplicationRemovalUseCaseImpl implements AdministerApplicationRemovalUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final co.edu.uco.seguridad.pdp.resources.application.rule.validator.ApplicationDeletionDependencyValidator resourceDependencies;
    private final co.edu.uco.seguridad.pdp.roles.application.rule.validator.ApplicationDeletionDependencyValidator roleDependencies;
    private final co.edu.uco.seguridad.pdp.profiles.application.rule.validator.ApplicationDeletionDependencyValidator profileDependencies;
    private final co.edu.uco.seguridad.pdp.assignments.application.rule.validator.ApplicationDeletionDependencyValidator assignmentDependencies;
    private final RemoveApplicationUseCase removeApplication;
    private final AdministrationAuditRepository audit;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public AdministerApplicationRemovalUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
                                                   co.edu.uco.seguridad.pdp.resources.application.rule.validator.ApplicationDeletionDependencyValidator resourceDependencies,
                                                   co.edu.uco.seguridad.pdp.roles.application.rule.validator.ApplicationDeletionDependencyValidator roleDependencies,
                                                   co.edu.uco.seguridad.pdp.profiles.application.rule.validator.ApplicationDeletionDependencyValidator profileDependencies,
                                                   co.edu.uco.seguridad.pdp.assignments.application.rule.validator.ApplicationDeletionDependencyValidator assignmentDependencies,
                                                   RemoveApplicationUseCase removeApplication, AdministrationAuditRepository audit,
                                                   IdentifierGenerator identifiers, TimeProvider time) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.resourceDependencies = Objects.requireNonNull(resourceDependencies);
        this.roleDependencies = Objects.requireNonNull(roleDependencies);
        this.profileDependencies = Objects.requireNonNull(profileDependencies);
        this.assignmentDependencies = Objects.requireNonNull(assignmentDependencies);
        this.removeApplication = Objects.requireNonNull(removeApplication, RequiredArgumentMessages.REMOVE_APPLICATION_USE_CASE);
        this.audit = Objects.requireNonNull(audit, RequiredArgumentMessages.ADMINISTRATION_AUDIT_REPOSITORY);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<Void> execute(AdministrationRequest input) {
        return mustBeAdministrator.execute(input)
                .then(resourceDependencies.execute(input.applicationId()))
                .then(roleDependencies.execute(input.applicationId()))
                .then(profileDependencies.execute(input.applicationId()))
                .then(assignmentDependencies.execute(input.applicationId()))
                .then(Mono.defer(() -> removeApplication.execute(input.applicationId())))
                .then(Mono.defer(() -> recordAudit(input, AdministrationOutcome.ALLOWED)))
                .onErrorResume(NotAuthorizedToAdministerException.class,
                        error -> recordAudit(input, AdministrationOutcome.DENIED).then(Mono.error(error)));
    }

    private Mono<Void> recordAudit(AdministrationRequest context, AdministrationOutcome outcome) {
        UUID eventId = identifiers.next();
        AdministrationEvent event = new AdministrationEvent(eventId, eventId.toString(), context.tenantId(),
                context.applicationId(), context.subject(), AdministrationOperation.APPLICATION_REMOVED, outcome, time.now());
        return Mono.defer(() -> audit.save(event)).onErrorResume(error -> Mono.empty());
    }
}
