package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto;

/**
 * Cuerpo de respuesta HTTP de OPA — {@code opa-response.schema.json}. Sin {@code decision_id}: el PDP genera el suyo.
 */
public record OpaResponse(OpaPolicyDecisionPayload result) {
}
