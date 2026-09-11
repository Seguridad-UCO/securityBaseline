package co.edu.uco.seguridad.pdp.applications.application.rule.validator;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/**
 * Punto de entrada único a cada regla que protege el registro de aplicaciones.
 *
 * <p>El caso de uso depende de este contrato en lugar de las reglas individuales, así que agregar una regla
 * más tarde es un cambio aquí y en ningún otro lugar.</p>
 */
public interface RegisterApplicationRulesValidator extends ReactiveOperationWithoutResult<RegisterApplicationRequest> {
}
