package co.edu.uco.seguridad.pdp.assignments.application.primaryport.response;

import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Salida del núcleo: value objects, sin aplanar. El aplanado es del adaptador web.
 */
public record AssignmentResponse(AssignmentId id, UserId userId, TenantId tenantId, ApplicationId applicationId,
                                 RoleId roleId, Instant validFrom, Optional<Instant> validUntil) {

    public AssignmentResponse {
        Objects.requireNonNull(id, RequiredArgumentMessages.ASSIGNMENT_ID);
        Objects.requireNonNull(userId, RequiredArgumentMessages.USER_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(roleId, RequiredArgumentMessages.ROLE_ID);
        Objects.requireNonNull(validFrom, RequiredArgumentMessages.VALID_FROM);
        Objects.requireNonNull(validUntil, RequiredArgumentMessages.VALID_UNTIL);
    }
}
