package co.edu.uco.seguridad.pdp.recursos.application.usecase;

import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.RegisterProtectedApplicationRequest;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.response.ProtectedApplicationResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Puerto primario: registrar una aplicación protegida y su primer recurso como unidad de trabajo.
 */
public interface RegisterProtectedApplicationUseCase
        extends ReactiveOperation<RegisterProtectedApplicationRequest, ProtectedApplicationResponse> {
}
