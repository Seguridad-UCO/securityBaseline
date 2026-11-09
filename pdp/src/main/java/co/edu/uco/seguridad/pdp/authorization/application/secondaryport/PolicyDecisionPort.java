package co.edu.uco.seguridad.pdp.authorization.application.secondaryport;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.PolicyEvaluationInput;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AccessDecision;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Puerto de salida hacia el motor de politicas. La frontera por donde entrara OPA en HU-004 sin
 * tocar el contrato HTTP: hoy la unica implementacion deniega por defecto (ADR-012).
 */
public interface PolicyDecisionPort extends ReactiveOperation<AccessRequest, AccessDecision> {

    /**
     * Frontera nueva para hechos enriquecidos; el método heredado permanece durante la migración interna.
     */
    default reactor.core.publisher.Mono<AccessDecision> decide(PolicyEvaluationInput input) {
        return execute(new AccessRequest(new co.edu.uco.seguridad.pdp.commons.model.TenantId(input.tenantId()),
                input.subjectId(), co.edu.uco.seguridad.pdp.commons.model.ApplicationId.of(input.applicationId()),
                new co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath(input.resourceId()),
                co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb.parse(input.action()), input.requestId(),
                input.correlationId(), java.util.Optional.empty(), input.roles()));
    }
}
