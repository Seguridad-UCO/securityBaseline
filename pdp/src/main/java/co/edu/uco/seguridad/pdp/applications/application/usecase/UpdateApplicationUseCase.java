package co.edu.uco.seguridad.pdp.applications.application.usecase;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.UpdateApplicationRequest;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface UpdateApplicationUseCase extends ReactiveOperation<UpdateApplicationRequest, RegisteredApplicationResponse> {
}
