package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto;

/**
 * Forma del campo {@code subject} de {@code policy-evaluation-input.schema.json}. Sin
 * {@code roles}/{@code profiles}/{@code entitlements}/{@code groups}/{@code attributes} a propósito
 * — ver PLAN-HU-006.md §0.
 */
public record OpaSubject(String id, String type, String tenantId) {
}
