package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto;

import java.util.List;
import java.util.Map;

/**
 * Forma del campo {@code subject} de {@code policy-evaluation-input.schema.json}. {@code roles}
 * (HU-008) lleva los nombres resueltos por el canal BFF — vacío en el canal interno, ver
 * PLAN-HU-008.md §0. Sin {@code profiles}/{@code entitlements}/{@code groups}/{@code attributes}
 * todavía — el catálogo del PDP no modela esos conceptos hoy.
 */
public record OpaSubject(String id, String type, String tenantId, List<String> roles, List<String> profiles,
        List<String> entitlements, List<String> groups, Map<String, Object> attributes) {
}
