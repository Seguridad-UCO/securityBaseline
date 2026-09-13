package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto;

/** Envoltorio {@code {"input": ...}} para la decisión de administración (HU-009). */
public record OpaAdministrationEvaluationRequest(OpaAdministrationEvaluationInput input) {
}
