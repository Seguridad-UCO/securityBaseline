package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListUserApplicationAssignmentsPageRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ProfileAssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListUserApplicationProfileAssignmentsPageUseCase;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

public final class ListUserApplicationProfileAssignmentsPageUseCaseImpl implements ListUserApplicationProfileAssignmentsPageUseCase {
    private final ProfileAssignmentRepository assignments;
    private final TimeProvider time;
    public ListUserApplicationProfileAssignmentsPageUseCaseImpl(ProfileAssignmentRepository assignments, TimeProvider time) { this.assignments = assignments; this.time = time; }
    @Override public Mono<ResultPage<ProfileAssignmentResponse>> execute(ListUserApplicationAssignmentsPageRequest input) {
        return assignments.findActivePageByUserAndApplication(input.userId(), input.tenantId(), input.applicationId(), time.now(), input.window()).map(page -> page.map(AssignmentPageResponses::profile));
    }
}
