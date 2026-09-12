package co.edu.uco.seguridad.pdp.assignments.application.usecase;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignRoleRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface AssignRoleUseCase extends ReactiveOperation<AssignRoleRequest, AssignmentResponse> {
}
