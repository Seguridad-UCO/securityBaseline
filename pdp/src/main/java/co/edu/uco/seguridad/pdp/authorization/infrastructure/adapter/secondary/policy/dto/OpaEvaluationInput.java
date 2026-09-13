package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto;

/**
 * Cuerpo {@code input} de {@code contracts/pdp-opa/v1/policy-evaluation-input.schema.json}. Solo los
 * campos requeridos por el schema más {@code resource.id} — ver PLAN-HU-006.md §0 y §7.
 */
import java.util.List;
import java.util.Map;

public record OpaEvaluationInput(String schemaVersion, OpaRequestInfo request, OpaSubject subject,
        OpaTenant tenant, OpaApplication application, OpaResource resource, String action,
        List<Map<String, Object>> relationships, Map<String, Object> context, Map<String, Object> security) {
}
