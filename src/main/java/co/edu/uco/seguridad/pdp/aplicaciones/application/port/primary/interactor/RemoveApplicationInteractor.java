package co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.interactor;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/**
 * Puerto primario publicado: eliminar una aplicación (compensación).
 * Un solo {@code execute}; no agrupa otras operaciones del módulo.
 */
public interface RemoveApplicationInteractor extends ReactiveOperationWithoutResult<ApplicationId> {
}
