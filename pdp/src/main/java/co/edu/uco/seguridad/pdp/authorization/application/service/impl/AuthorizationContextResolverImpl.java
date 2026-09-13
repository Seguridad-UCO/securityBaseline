package co.edu.uco.seguridad.pdp.authorization.application.service.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.PolicyEvaluationInput;
import co.edu.uco.seguridad.pdp.authorization.application.service.AuthorizationContextResolver;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Primera composición: solo proyecta hechos ya autenticados o resueltos por el flujo existente. */
public final class AuthorizationContextResolverImpl implements AuthorizationContextResolver {

    @Override
    public Mono<PolicyEvaluationInput> execute(AccessRequest input) {
        return Mono.just(new PolicyEvaluationInput(input.requestId(), input.correlationId(), input.subject(), "USER",
                input.tenantId().value(), input.subjectRoles(), Set.of(), Set.of(), Set.of(), Map.of(),
                input.tenantId().value(), Map.of(), input.applicationId().value().toString(), Map.of(), "http",
                input.resourcePath().value(), Map.of("path", input.resourcePath().value(), "method", input.action().name()),
                input.action().name(), List.of(), context(input), Map.of()));
    }

    private static Map<String, Object> context(AccessRequest input) {
        if (input.timestamp() == null) return Map.of();
        return Map.of("timestamp", input.timestamp().toString(), "environment", input.environment(),
                "method", input.contextMethod().name(), "channel", input.channel());
    }
}
