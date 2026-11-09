package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto;

/**
 * Forma del campo {@code request} de {@code policy-evaluation-input.schema.json}.
 */
public record OpaRequestInfo(String id, String correlationId) {
}
