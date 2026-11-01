package co.edu.uco.seguridad.pdp.authorization.infrastructure.serviceidentity;

import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.port.ServiceClientProvisioner;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/** Provisión de cliente confidencial para client_credentials; Keycloak es el único emisor de JWT. */
public final class KeycloakServiceClientProvisioner implements ServiceClientProvisioner {
    private final KeycloakServiceIdentityProperties properties;
    private final WebClient client;
    private final SecureRandom random = new SecureRandom();
    public KeycloakServiceClientProvisioner(KeycloakServiceIdentityProperties properties, WebClient.Builder builder) {
        this.properties=properties;
        this.client=properties.baseUrl() == null ? builder.clone().build() : builder.clone().baseUrl(properties.baseUrl().toString()).build();
    }
    @Override public Mono<ProvisionedServiceClient> provision(String clientId) {
        if (!properties.enabled()) return Mono.error(new IllegalStateException("el aprovisionamiento Keycloak de identidades de servicio está deshabilitado"));
        String secret = secret();
        Map<String, Object> audienceMapper = Map.of(
                "name", "pdp-audience", "protocol", "openid-connect", "protocolMapper", "oidc-audience-mapper",
                "config", Map.of("included.client.audience", properties.evidenceAudience(), "access.token.claim", "true"));
        Map<String, Object> representation = Map.of(
                "clientId", clientId, "enabled", true, "protocol", "openid-connect", "publicClient", false,
                "serviceAccountsEnabled", true, "standardFlowEnabled", false, "directAccessGrantsEnabled", false,
                "secret", secret, "defaultClientScopes", List.of("security-baseline-claims"),
                "protocolMappers", List.of(audienceMapper));
        return adminToken().flatMap(token -> client.post().uri("/admin/realms/{realm}/clients", properties.realm())
                .headers(headers -> headers.setBearerAuth(token)).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(representation)
                .exchangeToMono(response -> response.statusCode().is2xxSuccessful() ? response.releaseBody().thenReturn(new ProvisionedServiceClient(clientId, secret))
                        : response.bodyToMono(String.class).defaultIfEmpty("").flatMap(body -> Mono.error(new IllegalStateException("Keycloak no pudo crear el cliente: " + response.statusCode().value() + " " + body))))
                .timeout(properties.timeout()));
    }
    @Override public Mono<Void> deprovision(String clientId) {
        if (!properties.enabled()) return Mono.empty();
        return adminToken().flatMap(token -> client.get().uri(uri -> uri.path("/admin/realms/{realm}/clients")
                        .queryParam("clientId", clientId).build(properties.realm()))
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve().bodyToMono(ClientRepresentation[].class)
                .flatMapMany(reactor.core.publisher.Flux::fromArray)
                .next()
                .flatMap(representation -> client.delete().uri("/admin/realms/{realm}/clients/{id}", properties.realm(), representation.id())
                        .headers(headers -> headers.setBearerAuth(token)).exchangeToMono(response -> response.statusCode().is2xxSuccessful() || response.statusCode().value() == 404
                                ? response.releaseBody() : response.createException().flatMap(Mono::error)))
                .then())
                .timeout(properties.timeout());
    }
    private Mono<String> adminToken() {
        return client.post().uri(properties.tokenUri()).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "client_credentials").with("client_id", properties.clientId()).with("client_secret", properties.clientSecret()))
                .retrieve().bodyToMono(TokenResponse.class).map(TokenResponse::accessToken).timeout(properties.timeout());
    }
    private String secret() { byte[] bytes=new byte[32]; random.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private record TokenResponse(String access_token) { String accessToken() { return access_token; } }
    private record ClientRepresentation(String id) { }
}
