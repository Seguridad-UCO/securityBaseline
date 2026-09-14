package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.secondary.http;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.ingress.application.port.secondary.ApplicationCredentialValidationPort;
import co.edu.uco.seguridad.pep.ingress.application.port.secondary.PdpServiceTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

/** HTTP adapter for HU-013. The PDP remains the sole owner of application secrets. */
public final class PdpApplicationCredentialValidationAdapter implements ApplicationCredentialValidationPort {
    private static final Logger LOG = LoggerFactory.getLogger(PdpApplicationCredentialValidationAdapter.class);
    private final WebClient client;
    private final PdpServiceTokenProvider serviceToken;

    public PdpApplicationCredentialValidationAdapter(WebClient client, PdpServiceTokenProvider serviceToken) {
        this.client = Objects.requireNonNull(client);
        this.serviceToken = Objects.requireNonNull(serviceToken);
    }

    @Override
    public Mono<Void> validate(String applicationId, String secret) {
        return serviceToken.token().flatMap(token -> client.post().uri("/internal/v1/applications/{applicationId}/credential-validations", applicationId)
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(token))
                .bodyValue(Map.of("secret", secret))
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) return response.releaseBody();
                    if (response.statusCode().value() == 400) {
                        LOG.atWarn().addKeyValue("event.name", "pep.application-credential.rejected")
                                .addKeyValue("applicationId", applicationId)
                                .addKeyValue("pdp.status", response.statusCode().value())
                                .log("PDP rechazó la credencial de aplicación");
                        return response.releaseBody().then(Mono.error(new EnforcementFailure(
                                EnforcementFailure.Kind.UNAUTHENTICATED, "INTEGRATION_TOKEN_INVALID")));
                    }
                    return response.releaseBody().then(Mono.error(new EnforcementFailure(
                            EnforcementFailure.Kind.UNAVAILABLE, "PDP_CREDENTIAL_VALIDATION_UNAVAILABLE")));
                })
                .onErrorMap(error -> !(error instanceof EnforcementFailure),
                        error -> new EnforcementFailure(EnforcementFailure.Kind.UNAVAILABLE,
                                "PDP_CREDENTIAL_VALIDATION_UNAVAILABLE")));
    }
}
