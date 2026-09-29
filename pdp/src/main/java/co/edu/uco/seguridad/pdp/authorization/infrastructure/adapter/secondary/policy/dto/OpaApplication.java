package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto;

/**
 * Forma del campo {@code application} de {@code policy-evaluation-input.schema.json}.
 */

import java.util.Map;

public record OpaApplication(String id, Map<String, Object> attributes) {
}
