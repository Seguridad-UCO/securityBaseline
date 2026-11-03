package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListApplicationAdministratorsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListApplicationAdministratorsUseCase;
import co.edu.uco.seguridad.pdp.assignments.domain.AssignmentCriteria;
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

/**
 * Lista los administradores activos (asignaciones del rol ADMIN) de una aplicación (HU-020).
 */
public final class ListApplicationAdministratorsUseCaseImpl implements ListApplicationAdministratorsUseCase {

    private static final RoleName ADMIN_ROLE_NAME = new RoleName("ADMIN");

    private final RoleLookupByNameInScopeValidator roleLookup;
    private final AssignmentRepository repository;
    private final TimeProvider time;

    public ListApplicationAdministratorsUseCaseImpl(RoleLookupByNameInScopeValidator roleLookup,
                                                    AssignmentRepository repository, TimeProvider time) {
        this.roleLookup = Objects.requireNonNull(roleLookup, RequiredArgumentMessages.ROLE_LOOKUP_BY_NAME_IN_SCOPE_VALIDATOR);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.ASSIGNMENT_REPOSITORY);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<List<AssignmentResponse>> execute(ListApplicationAdministratorsRequest input) {
        RoleScope scope = RoleScope.ofApplication(input.tenantId(), input.applicationId());
        return roleLookup.execute(new RoleNameInScopeQuery(ADMIN_ROLE_NAME, scope))
                .flatMap(adminRoleId -> activeAdministrators(input, adminRoleId));
    }

    private Mono<List<AssignmentResponse>> activeAdministrators(ListApplicationAdministratorsRequest input,
                                                                RoleId adminRoleId) {
        Instant now = time.now();
        return repository.findBy(AssignmentCriteria.of(adminRoleId, input.tenantId()), PageWindow.defaultWindow())
                .map(page -> page.content().stream()
                        .filter(assignment -> assignment.isActive(now))
                        .map(assignment -> new AssignmentResponse(assignment.id(), assignment.userId(),
                                assignment.tenantId(), assignment.applicationId(), assignment.roleId(),
                                assignment.validity().validFrom(), assignment.validity().validUntil()))
                        .toList());
    }
}
