package co.edu.uco.seguridad.pdp.assignments.application.rule.validator;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignmentOwnershipQuery;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Resuelve a qué aplicación pertenece una asignación (HU-018). A diferencia de
 * {@code RoleApplicationLookupValidator} (HU-016), nunca vacío: {@code Assignment.applicationId} es
 * obligatorio desde HU-005. Rechaza con {@code AssignmentNotFoundException} si la asignación no
 * existe para ese inquilino.
 */
public interface AssignmentApplicationLookupValidator extends ReactiveOperation<AssignmentOwnershipQuery, ApplicationId> {
}
