package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListUserApplicationAssignmentsPageRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListUserApplicationRoleAssignmentsPageUseCase;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

public final class ListUserApplicationRoleAssignmentsPageUseCaseImpl implements ListUserApplicationRoleAssignmentsPageUseCase {
    private final AssignmentRepository assignments;
    private final TimeProvider time;

    public ListUserApplicationRoleAssignmentsPageUseCaseImpl(AssignmentRepository assignments, TimeProvider time) {
        this.assignments = assignments;
        this.time = time;
    }

    @Override
    public Mono<ResultPage<AssignmentResponse>> execute(ListUserApplicationAssignmentsPageRequest input) {
        return assignments.findActivePageByUserAndApplication(input.userId(), input.tenantId(), input.applicationId(), time.now(), input.window()).map(page -> page.map(AssignmentPageResponses::role));
    }
}
