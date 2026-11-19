package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto;

import java.util.List;

/**
 * Forma de {@code result} en {@code opa-response.schema.json} / {@code policy-decision.schema.json}.
 * Sin {@code obligations} a propósito — fuera de alcance de HU-006, ver PLAN-HU-006.md §7.
 */
public record OpaPolicyDecisionPayload(String effect, String reasonCode, List<OpaPolicyReference> policyReferences) {
}
