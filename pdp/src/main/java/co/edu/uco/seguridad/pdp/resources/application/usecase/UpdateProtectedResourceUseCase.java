package co.edu.uco.seguridad.pdp.resources.application.usecase;

import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.UpdateProtectedResourceRequest;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.response.RegisteredProtectedResourceResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface UpdateProtectedResourceUseCase extends ReactiveOperation<UpdateProtectedResourceRequest, RegisteredProtectedResourceResponse> { }
