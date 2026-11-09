package co.edu.uco.seguridad.pdp.assignments.application.primaryport.response;

import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record ProfileAssignmentResponse(ProfileAssignmentId id, UserId userId, TenantId tenantId,
                                        ApplicationId applicationId, ProfileId profileId,
                                        Set<AssignmentId> generatedAssignmentIds,
                                        Instant validFrom, Optional<Instant> validUntil) {

    public ProfileAssignmentResponse {
        Objects.requireNonNull(id, RequiredArgumentMessages.PROFILE_ASSIGNMENT_ID);
        Objects.requireNonNull(userId, RequiredArgumentMessages.USER_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(profileId, RequiredArgumentMessages.PROFILE_ID);
        generatedAssignmentIds = Set.copyOf(
                Objects.requireNonNull(generatedAssignmentIds, RequiredArgumentMessages.GENERATED_ASSIGNMENT_IDS));
        Objects.requireNonNull(validFrom, RequiredArgumentMessages.VALID_FROM);
        Objects.requireNonNull(validUntil, RequiredArgumentMessages.VALID_UNTIL);
    }
}
