package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto;

/**
 * Cuerpo {@code input} de la decisión de administración (HU-009) — sin {@code resource}/{@code action}:
 * a diferencia de {@link OpaEvaluationInput}, no está atado al contrato {@code pdp-opa/v1}.
 */
public record OpaAdministrationEvaluationInput(OpaSubject subject, OpaTenant tenant, OpaApplication application) {
}
