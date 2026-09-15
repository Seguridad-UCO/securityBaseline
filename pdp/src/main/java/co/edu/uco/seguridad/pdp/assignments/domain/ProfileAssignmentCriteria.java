package co.edu.uco.seguridad.pdp.assignments.domain;

import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/** Specification de consulta del catálogo de asignaciones de un perfil, aislado por tenant. */
public record ProfileAssignmentCriteria(ProfileId profileId, TenantId tenantId) {

    public ProfileAssignmentCriteria {
        Objects.requireNonNull(profileId, RequiredArgumentMessages.PROFILE_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
    }

    public static ProfileAssignmentCriteria of(ProfileId profileId, TenantId tenantId) {
        return new ProfileAssignmentCriteria(profileId, tenantId);
    }

    public boolean matches(ProfileAssignment profileAssignment) {
        Objects.requireNonNull(profileAssignment, RequiredArgumentMessages.PROFILE_ASSIGNMENT);
        return profileAssignment.profileId().equals(profileId) && profileAssignment.tenantId().equals(tenantId);
    }
}
