package co.edu.uco.seguridad.pdp.authorization.application.usecase;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ProfileAssignmentResponse;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileAssignmentCreationRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface AdministerProfileAssignmentCreationUseCase
        extends ReactiveOperation<AdministerProfileAssignmentCreationRequest, ProfileAssignmentResponse> {
}
