package co.edu.uco.seguridad.pdp.assignments.application.rule.validator;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeProfileAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignment;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Devuelve el ProfileAssignment encontrado para que el caso de uso lo revoque en cascada.
 */
public interface RevokeProfileAssignmentRulesValidator extends ReactiveOperation<RevokeProfileAssignmentRequest, ProfileAssignment> {
}
