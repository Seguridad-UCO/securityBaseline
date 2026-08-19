package co.edu.uco.seguridad.pdp.applications.application.usecase;

import co.edu.uco.seguridad.pdp.applications.application.port.primary.dto.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.applications.application.port.primary.dto.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Puerto primario: registrar una nueva aplicación para un inquilino activo.
 *
 * <p>Un solo {@code execute} garantiza que esta interfaz nunca agrupe más de una responsabilidad.
 * La implementación vive en {@code application/usecase/impl} y es invisible para el llamador.</p>
 */
public interface RegisterApplicationUseCase
        extends ReactiveOperation<RegisterApplicationRequest, RegisteredApplicationResponse> {
}
