package co.edu.uco.seguridad.pdp.assignments.application.rule.validator;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignProfileRequest;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

import java.util.Set;

/**
 * Devuelve el conjunto de roles ya validado (perfil existente, sin asignación activa duplicada).
 */
public interface AssignProfileRulesValidator extends ReactiveOperation<AssignProfileRequest, Set<RoleId>> {
}
