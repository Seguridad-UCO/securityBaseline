package co.edu.uco.seguridad.pdp.assignments.application.usecase;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ApplicationAssignmentCountsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ApplicationAssignmentCountsResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface ReadApplicationAssignmentCountsUseCase extends ReactiveOperation<ApplicationAssignmentCountsRequest, ApplicationAssignmentCountsResponse> {
}
