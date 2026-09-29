package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ApplicationAssignmentCountsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ApplicationAssignmentCountsResponse;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ReadApplicationAssignmentCountsUseCase;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.RoleNameInScopeQuery;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleLookupByNameInScopeValidator;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

public final class ReadApplicationAssignmentCountsUseCaseImpl implements ReadApplicationAssignmentCountsUseCase {
    private static final RoleName ADMIN = new RoleName("ADMIN");
    private final AssignmentRepository assignments;
    private final ProfileAssignmentRepository profileAssignments;
    private final RoleLookupByNameInScopeValidator roles;
    private final TimeProvider time;

    public ReadApplicationAssignmentCountsUseCaseImpl(AssignmentRepository assignments,
                                                       ProfileAssignmentRepository profileAssignments,
                                                       RoleLookupByNameInScopeValidator roles,
                                                       TimeProvider time) {
        this.assignments = assignments;
        this.profileAssignments = profileAssignments;
        this.roles = roles;
        this.time = time;
    }

    @Override
    public Mono<ApplicationAssignmentCountsResponse> execute(ApplicationAssignmentCountsRequest request) {
        return Mono.zip(
                assignments.countByApplication(request.tenantId(), request.applicationId()),
                profileAssignments.countByApplication(request.tenantId(), request.applicationId()),
                roles.execute(new RoleNameInScopeQuery(ADMIN,
                                RoleScope.ofApplication(request.tenantId(), request.applicationId())))
                        .flatMap(role -> assignments.countActiveByRoleAndApplication(role, request.tenantId(),
                                request.applicationId(), time.now())))
                .map(counts -> new ApplicationAssignmentCountsResponse(counts.getT1(), counts.getT2(), counts.getT3()));
    }
}
