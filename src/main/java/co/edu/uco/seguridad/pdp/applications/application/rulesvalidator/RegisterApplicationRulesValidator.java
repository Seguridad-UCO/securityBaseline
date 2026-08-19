package co.edu.uco.seguridad.pdp.applications.application.rulesvalidator;

import co.edu.uco.seguridad.pdp.applications.application.port.primary.dto.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/**
 * Punto de entrada único a cada regla que protege el registro de aplicaciones.
 *
 * <p>El caso de uso depende de este contrato en lugar de las reglas individuales, así que agregar una regla
 * más tarde es un cambio aquí y en ningún otro lugar.</p>
 */
public interface RegisterApplicationRulesValidator extends ReactiveOperationWithoutResult<RegisterApplicationRequest> {
}
