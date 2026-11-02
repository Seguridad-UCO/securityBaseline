package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerUserTenantAssignmentRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.TenantAdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustAdministerAtLeastOneApplicationValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerUserTenantAssignmentUseCase;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerTenantCatalogException;
import co.edu.uco.seguridad.pdp.authorization.domain.rule.UserTenantAssignmentMustStayInActorTenantRule;
import co.edu.uco.seguridad.pdp.authorization.domain.rule.model.UserTenantAssignment;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.response.UserResponse;
import co.edu.uco.seguridad.pdp.identity.application.usecase.AssignTenantUseCase;
import co.edu.uco.seguridad.pdp.identity.application.usecase.FindUserByIdUseCase;
import co.edu.uco.seguridad.shared.audit.AdministrationAuditRepository;
import co.edu.uco.seguridad.shared.audit.AdministrationEvent;
import co.edu.uco.seguridad.shared.audit.AdministrationOperation;
import co.edu.uco.seguridad.shared.audit.AdministrationOutcome;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Reasignar el inquilino de un usuario (SU-01). Antes el endpoint no tenía ningún control: cualquier
 * usuario autenticado podía mover a cualquier persona a otro inquilino. Ahora exige ser actor de
 * inquilino (ADR-036) y que el usuario y el inquilino destino sean los del actor
 * ({@link UserTenantAssignmentMustStayInActorTenantRule}). Se audita {@code USER_TENANT_ASSIGNED}
 * atribuido a la aplicación que concedió el permiso, igual que las escrituras de alcance inquilino de
 * HU-036; la auditoría es best-effort (HU-021) y nunca cambia el resultado.
 */
public final class AdministerUserTenantAssignmentUseCaseImpl implements AdministerUserTenantAssignmentUseCase {

    private final PrincipalMustAdministerAtLeastOneApplicationValidator mustAdministerSomeApplication;
    private final FindUserByIdUseCase users;
    private final UserTenantAssignmentMustStayInActorTenantRule mustStayInActorTenant;
    private final AssignTenantUseCase assignTenant;
    private final AdministrationAuditRepository audit;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public AdministerUserTenantAssignmentUseCaseImpl(
            PrincipalMustAdministerAtLeastOneApplicationValidator mustAdministerSomeApplication, FindUserByIdUseCase users,
            UserTenantAssignmentMustStayInActorTenantRule mustStayInActorTenant, AssignTenantUseCase assignTenant,
            AdministrationAuditRepository audit, IdentifierGenerator identifiers, TimeProvider time) {
        this.mustAdministerSomeApplication = Objects.requireNonNull(mustAdministerSomeApplication,
                RequiredArgumentMessages.PRINCIPAL_MUST_ADMINISTER_AT_LEAST_ONE_APPLICATION_VALIDATOR);
        this.users = Objects.requireNonNull(users, RequiredArgumentMessages.FIND_USER_BY_ID_USE_CASE);
        this.mustStayInActorTenant = Objects.requireNonNull(mustStayInActorTenant,
                RequiredArgumentMessages.USER_TENANT_ASSIGNMENT_RULE);
        this.assignTenant = Objects.requireNonNull(assignTenant, RequiredArgumentMessages.ASSIGN_TENANT_USE_CASE);
        this.audit = Objects.requireNonNull(audit, RequiredArgumentMessages.ADMINISTRATION_AUDIT_REPOSITORY);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<UserResponse> execute(AdministerUserTenantAssignmentRequest input) {
        TenantAdministrationRequest actor = input.actor();
        return mustAdministerSomeApplication.execute(actor)
                .onErrorResume(NotAuthorizedToAdministerTenantCatalogException.class,
                        error -> auditDenial(actor, error.evaluatedApplication()).then(Mono.error(error)))
                .flatMap(grantedBy -> users.execute(input.assignment().userId())
                        .doOnNext(user -> mustStayInActorTenant.execute(new UserTenantAssignment(
                                actor.tenantId(), user.tenantId(), input.assignment().tenantId())))
                        .then(Mono.defer(() -> assignTenant.execute(input.assignment())))
                        .flatMap(updated -> recordAudit(actor, grantedBy, AdministrationOutcome.ALLOWED).thenReturn(updated))
                        .onErrorResume(NotAuthorizedToAdministerTenantCatalogException.class,
                                error -> recordAudit(actor, grantedBy, AdministrationOutcome.DENIED).then(Mono.error(error))));
    }

    private Mono<Void> auditDenial(TenantAdministrationRequest actor, Optional<ApplicationId> evaluated) {
        return evaluated.map(application -> recordAudit(actor, application, AdministrationOutcome.DENIED))
                .orElseGet(Mono::empty);
    }

    private Mono<Void> recordAudit(TenantAdministrationRequest actor, ApplicationId applicationId, AdministrationOutcome outcome) {
        UUID eventId = identifiers.next();
        AdministrationEvent event = new AdministrationEvent(eventId, eventId.toString(), actor.tenantId(), applicationId,
                actor.subject(), AdministrationOperation.USER_TENANT_ASSIGNED, outcome, time.now());
        return Mono.defer(() -> audit.save(event)).onErrorResume(error -> Mono.empty());
    }
}
