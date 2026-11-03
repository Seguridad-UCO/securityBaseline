package co.edu.uco.seguridad.pdp.assignments.application.primaryport.request;

import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada de {@code ProfileAssignmentApplicationLookupValidator} (HU-019): la asignación de perfil y el inquilino que pregunta por ella.
 */
public record ProfileAssignmentOwnershipQuery(ProfileAssignmentId profileAssignmentId, TenantId tenantId) {

    public ProfileAssignmentOwnershipQuery {
        Objects.requireNonNull(profileAssignmentId, RequiredArgumentMessages.PROFILE_ASSIGNMENT_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
    }
}
