package co.edu.uco.seguridad.pdp.assignments.application.usecase;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListActiveAssignmentsPageRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ProfileAssignmentResponse;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface ListActiveProfileAssignmentsByProfilePageUseCase
        extends ReactiveOperation<ListActiveAssignmentsPageRequest<ProfileId>, ResultPage<ProfileAssignmentResponse>> {
}
