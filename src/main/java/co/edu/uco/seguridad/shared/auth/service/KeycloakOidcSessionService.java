package co.edu.uco.seguridad.shared.auth.service;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Service
public final class KeycloakOidcSessionService {

    private static final String KEYCLOAK_REGISTRATION_ID = "keycloak";

    private final ReactiveClientRegistrationRepository clientRegistrations;

    public KeycloakOidcSessionService(ReactiveClientRegistrationRepository clientRegistrations) {
        this.clientRegistrations = Objects.requireNonNull(clientRegistrations, "clientRegistrations");
    }

    public Mono<String> logoutUri(String idToken, String fallbackRedirectUri) {
        return clientRegistrations.findByRegistrationId(KEYCLOAK_REGISTRATION_ID)
                .map(client -> logoutUri(client, idToken, fallbackRedirectUri))
                .defaultIfEmpty(fallbackRedirectUri);
    }

    private String logoutUri(ClientRegistration client, String idToken, String fallbackRedirectUri) {
        Object endpoint = client.getProviderDetails().getConfigurationMetadata().get("end_session_endpoint");
        if (endpoint == null) {
            return fallbackRedirectUri;
        }
        var builder = UriComponentsBuilder.fromUriString(endpoint.toString())
                .queryParam("client_id", client.getClientId())
                .queryParam("post_logout_redirect_uri", fallbackRedirectUri);
        if (StringUtils.hasText(idToken)) {
            builder.queryParam("id_token_hint", idToken);
        }
        return builder.encode(StandardCharsets.UTF_8).build().toUriString();
    }
}
