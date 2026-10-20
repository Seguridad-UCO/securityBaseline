package co.edu.uco.seguridad.pdp.authorization.application.service.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveAuthorizationSubjectFactsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ResolveAuthorizationSubjectFactsUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.PolicyEvaluationInput;
import co.edu.uco.seguridad.pdp.authorization.application.service.AuthorizationContextResolver;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Primera composición: solo proyecta hechos ya autenticados o resueltos por el flujo existente.
 */
public final class AuthorizationContextResolverImpl implements AuthorizationContextResolver {
    private final ResolveAuthorizationSubjectFactsUseCase facts;
    private final ProtectedResourceRepository resources;

    public AuthorizationContextResolverImpl(ResolveAuthorizationSubjectFactsUseCase facts, ProtectedResourceRepository resources) {
        this.facts = facts;
        this.resources = resources;
    }

    /**
     * Compatibilidad para pruebas de proyección sin catálogos.
     */
    public AuthorizationContextResolverImpl() {
        this.facts = null;
        this.resources = null;
    }

    @Override
    public Mono<PolicyEvaluationInput> execute(AccessRequest input) {
        if (facts == null || input.subjectUserId().isEmpty())
            return Mono.just(project(input, input.subjectRoles(), Set.of(), Set.of(), null));
        return Mono.zip(facts.execute(new ResolveAuthorizationSubjectFactsRequest(input.subjectUserId().get(), input.tenantId(), input.applicationId())),
                        resources.findByApplicationPathAndMethod(input.applicationId(), input.resourcePath(), input.action()))
                .map(values -> project(input, values.getT1().roleNames(), values.getT1().profileNames(),
                        values.getT1().effectiveResourceIds().stream().map(id -> "resource:" + id.value()).collect(java.util.stream.Collectors.toSet()), values.getT2()));
    }

    private static PolicyEvaluationInput project(AccessRequest input, Set<String> roles, Set<String> profiles, Set<String> entitlements, ProtectedResource resource) {
        Map<String, Object> attributes = new java.util.HashMap<>();
        attributes.put("path", input.resourcePath().value());
        attributes.put("method", input.action().name());
        if (resource != null) {
            String resourceId = resource.id().value().toString();
            attributes.put("resourceId", resourceId);
            attributes.put("entitlementKey", "resource:" + resourceId);
            attributes.put("pathTemplate", resource.path().value());
        }
        // OPA must target the registered resource (the template), never a concrete path value
        // such as /employees/42. The original request path remains available in attributes.path.
        String policyResourceId = resource == null ? input.resourcePath().value() : resource.path().value();
        return new PolicyEvaluationInput(input.requestId(), input.correlationId(), input.subject(), "USER", input.tenantId().value(), roles, profiles, entitlements, Set.of(), Map.of(), input.tenantId().value(), Map.of(), input.applicationId().value().toString(), Map.of(), "http", policyResourceId, Map.copyOf(attributes), input.action().name(), List.of(), context(input), Map.of());
    }

    private static Map<String, Object> context(AccessRequest input) {
        if (input.timestamp() == null) return Map.of();
        return Map.of("timestamp", input.timestamp().toString(), "environment", input.environment(),
                "method", input.contextMethod().name(), "channel", input.channel());
    }
}
