package co.edu.uco.seguridad.pdp.applications.application.usecase;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/**
 * Puerto primario: eliminar una aplicación previamente registrada.
 *
 * <p>Operación compensatoria del módulo: se invoca cuando el registro de un recurso protegido
 * en {@code resources} falla después de que la aplicación ya fue creada.</p>
 */
public interface RemoveApplicationUseCase
        extends ReactiveOperationWithoutResult<ApplicationId> {
}
