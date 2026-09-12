package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AccessDecision;
import co.edu.uco.seguridad.pdp.authorization.application.secondaryport.PolicyDecisionPort;
import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.PolicyReference;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto.OpaApplication;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto.OpaEvaluationInput;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto.OpaEvaluationRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto.OpaRequestInfo;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto.OpaResource;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto.OpaResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto.OpaSubject;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto.OpaTenant;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.properties.OpaProperties;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;

/**
 * Cliente real de OPA sobre {@link PolicyDecisionPort} (HU-006, D9 del handoff PDP-PEP-OPA).
 * Traduce {@link AccessRequest} al contrato {@code contracts/pdp-opa/v1} y la respuesta de vuelta a
 * {@link AccessDecision}. No decide nada: cualquier fallo de red, timeout o de mapeo se propaga como
 * error — {@code AuthorizeUseCaseImpl} ya lo convierte en {@code INDETERMINATE}.
 */
public final class OpaPolicyDecisionAdapter implements PolicyDecisionPort {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final OpaProperties properties;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public OpaPolicyDecisionAdapter(WebClient webClient, ObjectMapper objectMapper, OpaProperties properties,
            IdentifierGenerator identifiers, TimeProvider time) {
        this.webClient = Objects.requireNonNull(webClient, RequiredArgumentMessages.OPA_WEB_CLIENT);
        this.objectMapper = Objects.requireNonNull(objectMapper, RequiredArgumentMessages.OBJECT_MAPPER);
        this.properties = Objects.requireNonNull(properties, RequiredArgumentMessages.OPA_PROPERTIES);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    private static final String SCHEMA_VERSION = "1.0";
    private static final String SUBJECT_TYPE = "USER";
    private static final String RESOURCE_TYPE = "http";

    @Override
    public Mono<AccessDecision> execute(AccessRequest input) {
        return Mono.defer(() -> {
            String requestBody = objectMapper.writeValueAsString(new OpaEvaluationRequest(toInput(input)));
            return webClient.post()
                    .uri(properties.decisionPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(properties.timeout())
                    .map(responseBody -> objectMapper.readValue(responseBody, OpaResponse.class))
                    .map(response -> toDecision(input, response));
        });
    }

    private static OpaEvaluationInput toInput(AccessRequest input) {
        return new OpaEvaluationInput(
                SCHEMA_VERSION,
                new OpaRequestInfo(input.requestId(), input.correlationId()),
                new OpaSubject(input.subject(), SUBJECT_TYPE, input.tenantId().value(), List.copyOf(input.subjectRoles())),
                new OpaTenant(input.tenantId().value()),
                new OpaApplication(input.applicationId().value().toString()),
                new OpaResource(RESOURCE_TYPE, input.resourcePath().value()),
                input.action().name());
    }

    private AccessDecision toDecision(AccessRequest input, OpaResponse response) {
        var payload = response.result();
        var policyReferences = payload.policyReferences().stream()
                .map(reference -> new PolicyReference(reference.id(), reference.version()))
                .toList();
        return new AccessDecision(identifiers.next(), DecisionState.valueOf(payload.effect()),
                ReasonCode.valueOf(payload.reasonCode()), policyReferences, input.requestId(), input.correlationId(),
                time.now());
    }
}
