package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto;

/** Forma del campo {@code resource} de {@code policy-evaluation-input.schema.json}. */
import java.util.Map;

public record OpaResource(String type, String id, Map<String, Object> attributes) {
}
