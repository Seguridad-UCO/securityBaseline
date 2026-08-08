package co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.interactor;

import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.dto.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.dto.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Puerto primario publicado: registrar una aplicación.
 * Un solo {@code execute}; no agrupa otras operaciones del módulo.
 */
public interface RegisterApplicationInteractor
        extends ReactiveOperation<RegisterApplicationRequest, RegisteredApplicationResponse> {
}
