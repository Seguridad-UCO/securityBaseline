package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AdministrationDecision;
import co.edu.uco.seguridad.pdp.authorization.application.secondaryport.AdministrationDecisionPort;
import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto.*;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.properties.OpaProperties;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Cliente OPA de la decisión de administración (HU-009), espejo de {@code OpaPolicyDecisionAdapter}
 * sobre {@link AdministrationDecisionPort}: mismo estilo de llamada, payload propio. No decide nada:
 * cualquier fallo de red, timeout o de mapeo se propaga como error —
 * {@code AuthorizeAdministrationUseCaseImpl} ya lo convierte en {@code INDETERMINATE}.
 */
public final class OpaAdministrationDecisionAdapter implements AdministrationDecisionPort {

    private static final String SUBJECT_TYPE = "USER";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final OpaProperties properties;

    public OpaAdministrationDecisionAdapter(WebClient webClient, ObjectMapper objectMapper, OpaProperties properties) {
        this.webClient = Objects.requireNonNull(webClient, RequiredArgumentMessages.OPA_WEB_CLIENT);
        this.objectMapper = Objects.requireNonNull(objectMapper, RequiredArgumentMessages.OBJECT_MAPPER);
        this.properties = Objects.requireNonNull(properties, RequiredArgumentMessages.OPA_PROPERTIES);
    }

    @Override
    public Mono<AdministrationDecision> execute(AdministrationRequest input) {
        return Mono.defer(() -> {
            String requestBody = objectMapper.writeValueAsString(new OpaAdministrationEvaluationRequest(toInput(input)));
            return webClient.post()
                    .uri(properties.administrationDecisionPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(properties.timeout())
                    .map(responseBody -> objectMapper.readValue(responseBody, OpaResponse.class))
                    .map(OpaAdministrationDecisionAdapter::toDecision);
        });
    }

    private static OpaAdministrationEvaluationInput toInput(AdministrationRequest input) {
        return new OpaAdministrationEvaluationInput(
                new OpaSubject(input.subject(), SUBJECT_TYPE, input.tenantId().value(), List.copyOf(input.subjectRoles()),
                        List.of(), List.of(), List.of(), Map.of()),
                new OpaTenant(input.tenantId().value(), Map.of()),
                new OpaApplication(input.applicationId().value().toString(), Map.of()));
    }

    private static AdministrationDecision toDecision(OpaResponse response) {
        var payload = response.result();
        return new AdministrationDecision(DecisionState.valueOf(payload.effect()), ReasonCode.valueOf(payload.reasonCode()));
    }
}
