package co.edu.uco.seguridad.pdp.resources.application.rulesvalidator;

import co.edu.uco.seguridad.pdp.resources.application.model.ProtectedResourceRegistration;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/** Coordina las reglas que protegen el registro en el catálogo, una vez la aplicación ya existe. */
public interface RegisterProtectedApplicationRulesValidator extends ReactiveOperationWithoutResult<ProtectedResourceRegistration> {
}
