package co.edu.uco.seguridad.shared.web.session;

import co.edu.uco.seguridad.shared.auth.service.KeycloakOidcSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakLogoutControllerTests {

    @Test
    void redirects_to_keycloak_logout_and_invalidates_the_local_session() {
        var controller = new KeycloakLogoutController(new KeycloakOidcSessionService(clientRegistrations()), "http://localhost:5173");
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/session/logout").build());
        var session = exchange.getSession().block();
        assertThat(session).isNotNull();
        session.getAttributes().put(KeycloakLogoutController.KEYCLOAK_ID_TOKEN_ATTRIBUTE, "id-token-value");

        controller.logout(exchange).block();

        assertThat(session.isExpired()).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).hasToString("302 FOUND");
        assertThat(exchange.getResponse().getHeaders().getLocation()).hasToString(
                "http://localhost:9090/realms/security-baseline/protocol/openid-connect/logout?client_id=security-baseline-bff&post_logout_redirect_uri=http://localhost:5173&id_token_hint=id-token-value");
    }

    @Test
    void falls_back_to_the_frontend_when_no_client_registration_is_available() {
        var controller = new KeycloakLogoutController(new KeycloakOidcSessionService(registrationId -> reactor.core.publisher.Mono.empty()), "http://localhost:5173");
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/session/logout").build());

        controller.logout(exchange).block();

        assertThat(exchange.getResponse().getStatusCode()).hasToString("302 FOUND");
        assertThat(exchange.getResponse().getHeaders().getLocation()).hasToString("http://localhost:5173");
    }

    private static InMemoryReactiveClientRegistrationRepository clientRegistrations() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("keycloak")
                .clientId("security-baseline-bff")
                .clientSecret("secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .issuerUri("http://localhost:9090/realms/security-baseline")
                .authorizationUri("http://localhost:9090/realms/security-baseline/protocol/openid-connect/auth")
                .tokenUri("http://localhost:9090/realms/security-baseline/protocol/openid-connect/token")
                .jwkSetUri("http://localhost:9090/realms/security-baseline/protocol/openid-connect/certs")
                .userInfoUri("http://localhost:9090/realms/security-baseline/protocol/openid-connect/userinfo")
                .userNameAttributeName("preferred_username")
                .providerConfigurationMetadata(java.util.Map.of(
                        "end_session_endpoint", "http://localhost:9090/realms/security-baseline/protocol/openid-connect/logout"))
                .build();
        return new InMemoryReactiveClientRegistrationRepository(registration);
    }
}
