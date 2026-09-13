package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.secondary.http;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.ingress.application.port.secondary.ApplicationCredentialValidationPort;
import co.edu.uco.seguridad.pep.ingress.infrastructure.integration.IntegrationProperties;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

/** HTTP adapter for HU-013. The PDP remains the sole owner of application secrets. */
public final class PdpApplicationCredentialValidationAdapter implements ApplicationCredentialValidationPort {
    private final WebClient client;
    private final IntegrationProperties properties;

    public PdpApplicationCredentialValidationAdapter(WebClient client, IntegrationProperties properties) {
        this.client = Objects.requireNonNull(client);
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public Mono<Void> validate(String applicationId, String secret) {
        return client.post().uri("/internal/v1/applications/{applicationId}/credential-validations", applicationId)
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(properties.pdpEvidenceToken()))
                .bodyValue(Map.of("secret", secret))
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) return response.releaseBody();
                    if (response.statusCode().value() == 400) {
                        return response.releaseBody().then(Mono.error(new EnforcementFailure(
                                EnforcementFailure.Kind.UNAUTHENTICATED, "INTEGRATION_TOKEN_INVALID")));
                    }
                    return response.releaseBody().then(Mono.error(new EnforcementFailure(
                            EnforcementFailure.Kind.UNAVAILABLE, "PDP_CREDENTIAL_VALIDATION_UNAVAILABLE")));
                })
                .onErrorMap(error -> !(error instanceof EnforcementFailure),
                        error -> new EnforcementFailure(EnforcementFailure.Kind.UNAVAILABLE,
                                "PDP_CREDENTIAL_VALIDATION_UNAVAILABLE"));
    }
}
