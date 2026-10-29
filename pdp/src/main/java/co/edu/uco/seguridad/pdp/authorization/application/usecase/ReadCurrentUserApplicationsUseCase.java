package co.edu.uco.seguridad.pdp.authorization.application.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ReadCurrentUserApplicationsRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.CurrentUserApplicationsResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface ReadCurrentUserApplicationsUseCase extends ReactiveOperation<ReadCurrentUserApplicationsRequest, CurrentUserApplicationsResponse> {
}
