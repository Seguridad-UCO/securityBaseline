package co.edu.uco.seguridad.pdp.assignments.application.usecase;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListActiveAssignmentsPageRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface ListActiveRoleAssignmentsByUserPageUseCase
        extends ReactiveOperation<ListActiveAssignmentsPageRequest<UserId>, ResultPage<AssignmentResponse>> {
}
