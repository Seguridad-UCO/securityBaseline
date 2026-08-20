package co.edu.uco.seguridad.shared.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakOidcSessionServiceTests {

    @Test
    void builds_a_logout_redirect_with_post_logout_return() {
        KeycloakOidcSessionService service = new KeycloakOidcSessionService(clientRegistrations());

        String location = service.logoutUri("id-token-value", "http://localhost:5173?registered=success").block();

        assertThat(location).isEqualTo(
                "http://localhost:9090/realms/security-baseline/protocol/openid-connect/logout?client_id=security-baseline-bff&post_logout_redirect_uri=http://localhost:5173?registered%3Dsuccess&id_token_hint=id-token-value");
    }

    @Test
    void falls_back_when_the_client_registration_is_missing() {
        ReactiveClientRegistrationRepository emptyClients = registrationId -> Mono.empty();
        KeycloakOidcSessionService service = new KeycloakOidcSessionService(emptyClients);

        assertThat(service.logoutUri(null, "http://localhost:5173?registered=success").block())
                .isEqualTo("http://localhost:5173?registered=success");
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
