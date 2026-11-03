package co.edu.uco.seguridad.pdp.assignments.application.rule.validator;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.domain.Assignment;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Finder con rechazo (mismo patrón que GrantResourceRulesValidator de HU-004): valida R5 y
 * devuelve la asignación encontrada, que el caso de uso transforma (revoke) y guarda.
 */
public interface RevokeAssignmentRulesValidator extends ReactiveOperation<RevokeAssignmentRequest, Assignment> {
}
