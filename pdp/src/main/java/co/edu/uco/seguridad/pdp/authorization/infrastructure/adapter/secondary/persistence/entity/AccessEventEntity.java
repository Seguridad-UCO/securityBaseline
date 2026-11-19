package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.entity;

/**
 * La forma de la fila, no del dominio: Strings planos.
 */
public record AccessEventEntity(String id, String decisionId, String requestId, String correlationId,
                                String tenantId, String applicationId, String subject, String resourcePath,
                                String action,
                                String state, String reasonCode, String occurredOn) {
}
