package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RemoveApplicationAdministratorRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.RemoveApplicationAdministratorUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.RevokeAssignmentUseCase;
import co.edu.uco.seguridad.pdp.assignments.domain.Assignment;
import co.edu.uco.seguridad.pdp.assignments.domain.AssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.LastAdministratorMustNotBeRevokedRule;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.model.AdministratorRevocationEligibility;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.RoleNameInScopeQuery;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleLookupByNameInScopeValidator;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Revoca la asignación del rol ADMIN de un usuario, rechazando si es el único administrador activo (HU-020). */
public final class RemoveApplicationAdministratorUseCaseImpl implements RemoveApplicationAdministratorUseCase {

    private static final RoleName ADMIN_ROLE_NAME = new RoleName("ADMIN");

    private final RoleLookupByNameInScopeValidator roleLookup;
    private final AssignmentRepository repository;
    private final LastAdministratorMustNotBeRevokedRule mustNotBeLastAdministrator;
    private final RevokeAssignmentUseCase revokeAssignment;
    private final TimeProvider time;

    public RemoveApplicationAdministratorUseCaseImpl(RoleLookupByNameInScopeValidator roleLookup,
            AssignmentRepository repository, LastAdministratorMustNotBeRevokedRule mustNotBeLastAdministrator,
            RevokeAssignmentUseCase revokeAssignment, TimeProvider time) {
        this.roleLookup = Objects.requireNonNull(roleLookup, RequiredArgumentMessages.ROLE_LOOKUP_BY_NAME_IN_SCOPE_VALIDATOR);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.ASSIGNMENT_REPOSITORY);
        this.mustNotBeLastAdministrator = Objects.requireNonNull(mustNotBeLastAdministrator,
                RequiredArgumentMessages.LAST_ADMINISTRATOR_MUST_NOT_BE_REVOKED_RULE);
        this.revokeAssignment = Objects.requireNonNull(revokeAssignment, RequiredArgumentMessages.REVOKE_ASSIGNMENT_USE_CASE);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<Void> execute(RemoveApplicationAdministratorRequest input) {
        RoleScope scope = RoleScope.ofApplication(input.tenantId(), input.applicationId());
        return roleLookup.execute(new RoleNameInScopeQuery(ADMIN_ROLE_NAME, scope))
                .flatMap(adminRoleId -> activeAdministrators(input, adminRoleId))
                .flatMap(activeAdministrators -> {
                    mustNotBeLastAdministrator.execute(
                            new AdministratorRevocationEligibility(input.applicationId(), activeAdministrators.size()));
                    return revokeTarget(input, activeAdministrators);
                });
    }

    private Mono<List<Assignment>> activeAdministrators(RemoveApplicationAdministratorRequest input, RoleId adminRoleId) {
        Instant now = time.now();
        return repository.findBy(AssignmentCriteria.of(adminRoleId, input.tenantId()), PageWindow.defaultWindow())
                .map(page -> page.content().stream().filter(assignment -> assignment.isActive(now)).toList());
    }

    private Mono<Void> revokeTarget(RemoveApplicationAdministratorRequest input, List<Assignment> activeAdministrators) {
        return activeAdministrators.stream()
                .filter(assignment -> assignment.userId().equals(input.userId()))
                .findFirst()
                .map(target -> revokeAssignment.execute(new RevokeAssignmentRequest(target.id(), input.tenantId())))
                .orElseGet(Mono::empty);
    }
}
