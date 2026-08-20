package co.edu.uco.seguridad.pdp.resources.application.usecase;

import co.edu.uco.seguridad.pdp.resources.application.port.primary.dto.request.RegisterProtectedResourceRequest;
import co.edu.uco.seguridad.pdp.resources.application.port.primary.dto.response.RegisteredProtectedResourceResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/** Puerto primario: registrar un endpoint protegido bajo una aplicación existente del inquilino. */
public interface RegisterProtectedResourceUseCase
        extends ReactiveOperation<RegisterProtectedResourceRequest, RegisteredProtectedResourceResponse> {
}
