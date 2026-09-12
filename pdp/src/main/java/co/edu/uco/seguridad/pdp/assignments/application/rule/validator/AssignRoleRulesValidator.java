package co.edu.uco.seguridad.pdp.assignments.application.rule.validator;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignRoleRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/** Punto de entrada único a las reglas que protegen la asignación de un rol (R1 a R4). */
public interface AssignRoleRulesValidator extends ReactiveOperationWithoutResult<AssignRoleRequest> {
}
