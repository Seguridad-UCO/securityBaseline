package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto;

/**
 * Envoltorio {@code {"input": ...}} — convención HTTP estándar del API de decisión de OPA.
 */
public record OpaEvaluationRequest(OpaEvaluationInput input) {
}
