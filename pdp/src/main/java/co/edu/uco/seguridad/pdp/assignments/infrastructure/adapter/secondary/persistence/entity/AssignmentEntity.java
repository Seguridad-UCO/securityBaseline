package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.entity;

/** La forma de la fila, no del dominio: Strings, y validUntil null si sigue vigente. */
public record AssignmentEntity(String id, String userId, String tenantId, String applicationId, String roleId,
        String validFrom, String validUntil) {
}
