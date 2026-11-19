package co.edu.uco.seguridad.pdp.assignments.application.usecase;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListProfileAssignmentsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ProfileAssignmentResponse;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface ListProfileAssignmentsUseCase
        extends ReactiveOperation<ListProfileAssignmentsRequest, ResultPage<ProfileAssignmentResponse>> {
}
