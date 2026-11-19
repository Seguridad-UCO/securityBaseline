package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto;

/**
 * Un elemento de {@code result.policyReferences} — ver {@code policy-decision.schema.json}.
 */
public record OpaPolicyReference(String id, String version) {
}
