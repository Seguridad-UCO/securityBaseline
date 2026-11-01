package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListProfileApplicationAssignmentsPageRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ProfileAssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListProfileApplicationAssignmentsPageUseCase;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

public final class ListProfileApplicationAssignmentsPageUseCaseImpl implements ListProfileApplicationAssignmentsPageUseCase {
    private final ProfileAssignmentRepository assignments;
    private final TimeProvider time;
    public ListProfileApplicationAssignmentsPageUseCaseImpl(ProfileAssignmentRepository assignments, TimeProvider time) { this.assignments = assignments; this.time = time; }
    @Override public Mono<ResultPage<ProfileAssignmentResponse>> execute(ListProfileApplicationAssignmentsPageRequest input) {
        return assignments.findActivePageByProfileAndApplication(input.profileId(), input.tenantId(), input.applicationId(), time.now(), input.window()).map(page -> page.map(AssignmentPageResponses::profile));
    }
}
