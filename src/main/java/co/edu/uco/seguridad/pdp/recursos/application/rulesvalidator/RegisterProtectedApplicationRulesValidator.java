package co.edu.uco.seguridad.pdp.recursos.application.rulesvalidator;

import co.edu.uco.seguridad.pdp.recursos.application.model.ProtectedResourceRegistration;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/**
 * Coordina las reglas que protegen el registro en el catálogo.
 *
 * <p>Un solo método, porque cada regla de este caso de uso necesita la aplicación registrada para
 * decidir, y eso solo está disponible después de que Aplicaciones ha aceptado el registro. La
 * validez del inquilino no se vuelve a verificar aquí: Aplicaciones ya la aplica con la misma regla
 * compartida, y repetirla sería una segunda decisión que podría desviarse.</p>
 */
public interface RegisterProtectedApplicationRulesValidator extends ReactiveOperationWithoutResult<ProtectedResourceRegistration> {
}
