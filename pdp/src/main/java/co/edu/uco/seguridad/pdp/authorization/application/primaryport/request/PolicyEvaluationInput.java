package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.authorization.domain.model.PolicyReference;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Hechos confiables, ya resueltos por el PDP, que se entregan al motor de políticas. */
public record PolicyEvaluationInput(String requestId, String correlationId, String subjectId, String subjectType,
        String subjectTenantId, Set<String> roles, Set<String> profiles, Set<String> entitlements, Set<String> groups,
        Map<String, Object> subjectAttributes, String tenantId, Map<String, Object> tenantAttributes,
        String applicationId, Map<String, Object> applicationAttributes, String resourceType, String resourceId,
        Map<String, Object> resourceAttributes, String action, List<Map<String, Object>> relationships,
        Map<String, Object> context, Map<String, Object> security) {

    public PolicyEvaluationInput {
        Objects.requireNonNull(requestId);
        Objects.requireNonNull(correlationId);
        Objects.requireNonNull(subjectId);
        Objects.requireNonNull(subjectType);
        Objects.requireNonNull(subjectTenantId);
        roles = Set.copyOf(Objects.requireNonNull(roles));
        profiles = Set.copyOf(Objects.requireNonNull(profiles));
        entitlements = Set.copyOf(Objects.requireNonNull(entitlements));
        groups = Set.copyOf(Objects.requireNonNull(groups));
        subjectAttributes = Map.copyOf(Objects.requireNonNull(subjectAttributes));
        Objects.requireNonNull(tenantId);
        tenantAttributes = Map.copyOf(Objects.requireNonNull(tenantAttributes));
        Objects.requireNonNull(applicationId);
        applicationAttributes = Map.copyOf(Objects.requireNonNull(applicationAttributes));
        Objects.requireNonNull(resourceType);
        Objects.requireNonNull(resourceId);
        resourceAttributes = Map.copyOf(Objects.requireNonNull(resourceAttributes));
        Objects.requireNonNull(action);
        relationships = List.copyOf(Objects.requireNonNull(relationships));
        context = Map.copyOf(Objects.requireNonNull(context));
        security = Map.copyOf(Objects.requireNonNull(security));
    }
}
