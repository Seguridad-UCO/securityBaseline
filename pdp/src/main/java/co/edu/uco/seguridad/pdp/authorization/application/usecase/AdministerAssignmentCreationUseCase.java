package co.edu.uco.seguridad.pdp.authorization.application.usecase;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerAssignmentCreationRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface AdministerAssignmentCreationUseCase
        extends ReactiveOperation<AdministerAssignmentCreationRequest, AssignmentResponse> {
}
