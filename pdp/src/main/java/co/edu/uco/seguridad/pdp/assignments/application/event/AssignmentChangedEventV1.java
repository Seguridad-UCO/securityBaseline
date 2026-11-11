package co.edu.uco.seguridad.pdp.assignments.application.event;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Contrato versionado e inmutable publicado a las aplicaciones consumidoras. */
public record AssignmentChangedEventV1(String eventId, int schemaVersion, String eventType, Instant occurredOn,
                                       String tenantId, String applicationId, String applicationName, String userId,
                                       String correlationId, String assignmentId, String roleId, String roleName,
                                       String profileAssignmentId, String profileId, String profileName,
                                       List<RoleAssignment> roles) {
    public AssignmentChangedEventV1 {
        Objects.requireNonNull(eventId); Objects.requireNonNull(eventType); Objects.requireNonNull(occurredOn);
        Objects.requireNonNull(tenantId); Objects.requireNonNull(applicationId); Objects.requireNonNull(applicationName);
        Objects.requireNonNull(userId);
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
    public record RoleAssignment(String assignmentId, String roleId, String roleName) {
        public RoleAssignment { Objects.requireNonNull(assignmentId); Objects.requireNonNull(roleId); Objects.requireNonNull(roleName); }
    }
}
