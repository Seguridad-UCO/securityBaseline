package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.secondary.http;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.ingress.application.port.secondary.PdpServiceTokenProvider;
import co.edu.uco.seguridad.pep.ingress.infrastructure.properties.PdpServiceIdentityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Obtiene y renueva el access token del service account sin exponerlo en logs ni propiedades.
 */
public final class KeycloakClientCredentialsTokenProvider implements PdpServiceTokenProvider {
    private static final Logger LOG = LoggerFactory.getLogger(KeycloakClientCredentialsTokenProvider.class);
    private final PdpServiceIdentityProperties properties;
    private final WebClient client;
    private CachedToken cached;
    private Mono<String> inFlight;
    private boolean missingConfigurationWarned;

    public KeycloakClientCredentialsTokenProvider(PdpServiceIdentityProperties properties, WebClient client) {
        this.properties = properties;
        this.client = client;
    }

    @Override
    public Mono<String> token() {
        return Mono.defer(this::currentOrRefresh);
    }

    private synchronized Mono<String> currentOrRefresh() {
        if (!properties.configured()) {
            if (!missingConfigurationWarned) {
                missingConfigurationWarned = true;
                LOG.warn("PEP service identity is not configured; protected requests will fail closed");
            }
            return unavailable();
        }
        if (cached != null && cached.validAt(Instant.now())) return Mono.just(cached.value());
        if (inFlight != null) return inFlight;
        inFlight = requestToken().doOnNext(this::cache)
                .doOnError(error -> LOG.warn("PEP service identity unavailable; protected requests will fail closed"))
                .doFinally(signal -> clearInFlight()).cache();
        return inFlight;
    }

    private Mono<String> requestToken() {
        return client.post().uri(properties.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                        .with("client_id", properties.clientId()).with("client_secret", properties.clientSecret()))
                .exchangeToMono(response -> response.statusCode().is2xxSuccessful()
                        ? response.bodyToMono(TokenResponse.class).flatMap(token -> valid(token)
                                                                                    ? Mono.just(token.accessToken()) : unavailable())
                        : response.releaseBody().then(unavailable()))
                .timeout(properties.timeout()).onErrorMap(error -> !(error instanceof EnforcementFailure), error ->
                        new EnforcementFailure(EnforcementFailure.Kind.UNAVAILABLE, "PDP_SERVICE_IDENTITY_UNAVAILABLE"));
    }

    private void cache(String value) {
        cached = new CachedToken(value, Instant.now().plusSeconds(Math.max(1,
                tokenLifetimeSeconds - properties.refreshSkew().toSeconds())));
    }

    private volatile long tokenLifetimeSeconds;

    private Mono<String> unavailable() {
        return Mono.error(new EnforcementFailure(EnforcementFailure.Kind.UNAVAILABLE,
                "PDP_SERVICE_IDENTITY_UNAVAILABLE"));
    }

    private synchronized void clearInFlight() {
        inFlight = null;
    }

    private boolean valid(TokenResponse response) {
        if (response == null || response.accessToken() == null || response.accessToken().isBlank() || response.expiresIn() < 1)
            return false;
        tokenLifetimeSeconds = response.expiresIn();
        return true;
    }

    private record TokenResponse(String access_token, long expires_in) {
        String accessToken() {
            return access_token;
        }

        long expiresIn() {
            return expires_in;
        }
    }

    private record CachedToken(String value, Instant refreshAt) {
        boolean validAt(Instant now) {
            return now.isBefore(refreshAt);
        }
    }
}
