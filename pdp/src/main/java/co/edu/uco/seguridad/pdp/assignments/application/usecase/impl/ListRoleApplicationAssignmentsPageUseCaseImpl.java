package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListRoleApplicationAssignmentsPageRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListRoleApplicationAssignmentsPageUseCase;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

public final class ListRoleApplicationAssignmentsPageUseCaseImpl implements ListRoleApplicationAssignmentsPageUseCase {
    private final AssignmentRepository assignments;
    private final TimeProvider time;

    public ListRoleApplicationAssignmentsPageUseCaseImpl(AssignmentRepository assignments, TimeProvider time) {
        this.assignments = assignments;
        this.time = time;
    }

    @Override
    public Mono<ResultPage<AssignmentResponse>> execute(ListRoleApplicationAssignmentsPageRequest input) {
        return assignments.findActivePageByRoleAndApplication(input.roleId(), input.tenantId(), input.applicationId(), time.now(), input.window()).map(page -> page.map(AssignmentPageResponses::role));
    }
}
