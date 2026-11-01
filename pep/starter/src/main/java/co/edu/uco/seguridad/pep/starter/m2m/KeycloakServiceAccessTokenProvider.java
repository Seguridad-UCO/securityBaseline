package co.edu.uco.seguridad.pep.starter.m2m;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

/** Cache de proceso, por identidad, para client_credentials. No persiste el token ni el secreto. */
final class KeycloakServiceAccessTokenProvider implements ServiceAccessTokenProvider {
    private final ServiceIdentityProperties properties;
    private final WebClient client;
    private CachedToken cached;
    private Mono<String> inFlight;

    KeycloakServiceAccessTokenProvider(ServiceIdentityProperties properties, WebClient client) {
        this.properties = properties;
        this.client = client;
    }

    @Override
    public Mono<String> token() {
        return Mono.defer(this::currentOrRefresh);
    }

    private synchronized Mono<String> currentOrRefresh() {
        if (!properties.configured()) return Mono.error(new IllegalStateException("SERVICE_IDENTITY_NOT_CONFIGURED"));
        if (cached != null && Instant.now().isBefore(cached.refreshAt())) return Mono.just(cached.value());
        if (inFlight != null) return inFlight;
        inFlight = client.post().uri(properties.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                        .with("client_id", properties.clientId()).with("client_secret", properties.clientSecret()))
                .exchangeToMono(response -> response.statusCode().is2xxSuccessful()
                        ? response.bodyToMono(TokenResponse.class)
                        : response.releaseBody().then(Mono.error(new IllegalStateException("SERVICE_IDENTITY_TOKEN_UNAVAILABLE"))))
                .flatMap(token -> token == null || token.access_token() == null || token.access_token().isBlank() || token.expires_in() < 1
                        ? Mono.error(new IllegalStateException("SERVICE_IDENTITY_TOKEN_INVALID"))
                        : Mono.just(token))
                .timeout(properties.timeout())
                .map(token -> {
                    String value = token.access_token();
                    cached = new CachedToken(value, Instant.now().plusSeconds(Math.max(1, token.expires_in() - properties.refreshSkew().toSeconds())));
                    return value;
                })
                .doFinally(signal -> clearInFlight()).cache();
        return inFlight;
    }

    private synchronized void clearInFlight() { inFlight = null; }

    private record TokenResponse(String access_token, long expires_in) { }
    private record CachedToken(String value, Instant refreshAt) { }
}
