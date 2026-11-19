package co.edu.uco.seguridad.pdp.assignments.application.usecase;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignProfileRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ProfileAssignmentResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface AssignProfileUseCase extends ReactiveOperation<AssignProfileRequest, ProfileAssignmentResponse> {
}
