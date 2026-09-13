package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.entity;

import java.util.List;

/** La forma de la fila, no del dominio: Strings, y validUntil null si sigue vigente. */
public record ProfileAssignmentEntity(String id, String userId, String tenantId, String applicationId,
        String profileId, List<String> generatedAssignmentIds, String validFrom, String validUntil) {
}
