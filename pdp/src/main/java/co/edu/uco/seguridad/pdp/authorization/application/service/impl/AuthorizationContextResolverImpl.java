package co.edu.uco.seguridad.pdp.authorization.application.service.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveAuthorizationSubjectFactsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ResolveAuthorizationSubjectFactsUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.PolicyEvaluationInput;
import co.edu.uco.seguridad.pdp.authorization.application.service.AuthorizationContextResolver;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.ProtectedResourceLookup;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.ProtectedResourceIdLookupValidator;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Primera composición: solo proyecta hechos ya autenticados o resueltos por el flujo existente.
 */
public final class AuthorizationContextResolverImpl implements AuthorizationContextResolver {
    private final ResolveAuthorizationSubjectFactsUseCase facts;
    private final ProtectedResourceIdLookupValidator resourceIds;

    public AuthorizationContextResolverImpl(ResolveAuthorizationSubjectFactsUseCase facts, ProtectedResourceIdLookupValidator resourceIds) {
        this.facts = facts;
        this.resourceIds = resourceIds;
    }

    /**
     * Compatibilidad para pruebas de proyección sin catálogos.
     */
    public AuthorizationContextResolverImpl() {
        this.facts = null;
        this.resourceIds = null;
    }

    @Override
    public Mono<PolicyEvaluationInput> execute(AccessRequest input) {
        if (facts == null || input.subjectUserId().isEmpty())
            return Mono.just(project(input, input.subjectRoles(), Set.of(), Set.of(), null));
        return Mono.zip(facts.execute(new ResolveAuthorizationSubjectFactsRequest(input.subjectUserId().get(), input.tenantId(), input.applicationId())),
                        resourceIds.execute(new ProtectedResourceLookup(input.applicationId(), input.resourcePath(), input.action())))
                .map(values -> project(input, values.getT1().roleNames(), values.getT1().profileNames(),
                        values.getT1().effectiveResourceIds().stream().map(id -> "resource:" + id.value()).collect(java.util.stream.Collectors.toSet()), values.getT2().value().toString()));
    }

    private static PolicyEvaluationInput project(AccessRequest input, Set<String> roles, Set<String> profiles, Set<String> entitlements, String resourceId) {
        Map<String, Object> attributes = new java.util.HashMap<>();
        attributes.put("path", input.resourcePath().value());
        attributes.put("method", input.action().name());
        if (resourceId != null) {
            attributes.put("resourceId", resourceId);
            attributes.put("entitlementKey", "resource:" + resourceId);
        }
        return new PolicyEvaluationInput(input.requestId(), input.correlationId(), input.subject(), "USER", input.tenantId().value(), roles, profiles, entitlements, Set.of(), Map.of(), input.tenantId().value(), Map.of(), input.applicationId().value().toString(), Map.of(), "http", input.resourcePath().value(), Map.copyOf(attributes), input.action().name(), List.of(), context(input), Map.of());
    }

    private static Map<String, Object> context(AccessRequest input) {
        if (input.timestamp() == null) return Map.of();
        return Map.of("timestamp", input.timestamp().toString(), "environment", input.environment(),
                "method", input.contextMethod().name(), "channel", input.channel());
    }
}
