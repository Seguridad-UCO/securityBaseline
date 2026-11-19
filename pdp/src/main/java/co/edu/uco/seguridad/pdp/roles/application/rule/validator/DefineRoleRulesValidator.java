package co.edu.uco.seguridad.pdp.roles.application.rule.validator;

import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.DefineRoleRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/**
 * Punto de entrada único a las reglas que protegen la definición de un rol (R1 y, si el alcance es de aplicación, R2).
 */
public interface DefineRoleRulesValidator extends ReactiveOperationWithoutResult<DefineRoleRequest> {
}
