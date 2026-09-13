package co.edu.uco.seguridad.pdp.assignments.domain;

import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.assignments.domain.model.Validity;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Entrada del catálogo de asignaciones de perfil (BC-08): liga un usuario, una aplicación y un
 * perfil, con una vigencia, y recuerda qué {@code Assignment} generó al materializarse — para poder
 * revocarlos en cascada (HU-011, decisión confirmada). Espejo de {@code Assignment}.
 */
public record ProfileAssignment(ProfileAssignmentId id, UserId userId, TenantId tenantId, ApplicationId applicationId,
        ProfileId profileId, Set<AssignmentId> generatedAssignmentIds, Validity validity) {

    public ProfileAssignment {
        Objects.requireNonNull(id, RequiredArgumentMessages.PROFILE_ASSIGNMENT_ID);
        Objects.requireNonNull(userId, RequiredArgumentMessages.USER_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(profileId, RequiredArgumentMessages.PROFILE_ID);
        generatedAssignmentIds = Set.copyOf(
                Objects.requireNonNull(generatedAssignmentIds, RequiredArgumentMessages.GENERATED_ASSIGNMENT_IDS));
        Objects.requireNonNull(validity, RequiredArgumentMessages.VALIDITY);
    }

    public static ProfileAssignment grant(ProfileAssignmentId id, UserId userId, TenantId tenantId,
            ApplicationId applicationId, ProfileId profileId, Set<AssignmentId> generatedAssignmentIds, Instant now) {
        return new ProfileAssignment(id, userId, tenantId, applicationId, profileId, generatedAssignmentIds,
                Validity.startingNow(now));
    }

    public ProfileAssignment revoke(Instant now) {
        return new ProfileAssignment(id, userId, tenantId, applicationId, profileId, generatedAssignmentIds,
                validity.endingAt(now));
    }

    public boolean isActive(Instant now) {
        return validity.isActiveAt(now);
    }
}
