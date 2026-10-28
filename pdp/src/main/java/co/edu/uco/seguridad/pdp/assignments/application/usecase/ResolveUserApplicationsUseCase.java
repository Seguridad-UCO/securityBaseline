package co.edu.uco.seguridad.pdp.assignments.application.usecase;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveUserApplicationsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.UserApplicationAssignmentsResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface ResolveUserApplicationsUseCase extends ReactiveOperation<ResolveUserApplicationsRequest, UserApplicationAssignmentsResponse> {
}
