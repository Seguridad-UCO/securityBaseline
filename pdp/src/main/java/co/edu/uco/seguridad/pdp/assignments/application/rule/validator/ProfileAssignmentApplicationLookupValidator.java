package co.edu.uco.seguridad.pdp.assignments.application.rule.validator;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ProfileAssignmentOwnershipQuery;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Resuelve a qué aplicación pertenece una asignación de perfil (HU-019). Nunca vacío — espejo
 * exacto de {@code AssignmentApplicationLookupValidator} (HU-018), aplicado a
 * {@code ProfileAssignment}.
 */
public interface ProfileAssignmentApplicationLookupValidator
        extends ReactiveOperation<ProfileAssignmentOwnershipQuery, ApplicationId> {
}
