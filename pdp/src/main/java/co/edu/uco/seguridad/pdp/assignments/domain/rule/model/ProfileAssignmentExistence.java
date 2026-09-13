package co.edu.uco.seguridad.pdp.assignments.domain.rule.model;

import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/** Hecho ya resuelto para ProfileAssignmentMustExistForTenantRule. */
public record ProfileAssignmentExistence(ProfileAssignmentId profileAssignmentId, TenantId tenantId, boolean registered) {

    public ProfileAssignmentExistence {
        Objects.requireNonNull(profileAssignmentId, RequiredArgumentMessages.PROFILE_ASSIGNMENT_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
    }
}
