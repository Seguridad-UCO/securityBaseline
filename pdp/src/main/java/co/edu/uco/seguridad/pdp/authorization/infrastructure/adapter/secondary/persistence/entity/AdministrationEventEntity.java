package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.entity;

/**
 * La forma de la fila de {@code administration_event} (HU-021), no del dominio: Strings planos.
 */
public record AdministrationEventEntity(String id, String correlationId, String tenantId, String applicationId,
                                        String subject, String operation, String outcome, String occurredOn) {
}
