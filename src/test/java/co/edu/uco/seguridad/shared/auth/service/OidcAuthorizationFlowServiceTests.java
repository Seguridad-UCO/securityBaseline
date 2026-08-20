package co.edu.uco.seguridad.shared.auth.service;

import co.edu.uco.seguridad.shared.auth.model.OidcFlowIntent;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.server.WebSessionOAuth2ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import static org.assertj.core.api.Assertions.assertThat;

class OidcAuthorizationFlowServiceTests {

    @Test
    void login_redirects_to_the_standard_authorization_request() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/oauth2/authorization/keycloak").build());
        OidcAuthorizationFlowService service = new OidcAuthorizationFlowService(clientRegistrations(),
                authorizationRequestRepository(), new OidcFlowStateService());

        service.beginLogin(exchange).block();

        assertThat(exchange.getResponse().getStatusCode()).hasToString("302 FOUND");
        String location = exchange.getResponse().getHeaders().getLocation().toString();
        assertThat(location)
                .startsWith("http://localhost:9090/realms/security-baseline/protocol/openid-connect/auth")
                .contains("client_id=security-baseline-bff")
                .doesNotContain("prompt=create")
                .doesNotContain("kc_action=register");
        Object flowIntent = exchange.getSession().block().getAttribute(OidcFlowStateService.FLOW_INTENT_ATTRIBUTE);
        assertThat((String) flowIntent).isEqualTo(OidcFlowIntent.LOGIN.name());
    }

    @Test
    void registration_redirects_to_keycloak_in_registration_mode() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/oauth2/authorization/keycloak/register").build());
        OidcAuthorizationFlowService service = new OidcAuthorizationFlowService(clientRegistrations(),
                authorizationRequestRepository(), new OidcFlowStateService());

        service.beginRegistration(exchange).block();

        assertThat(exchange.getResponse().getStatusCode()).hasToString("302 FOUND");
        String location = exchange.getResponse().getHeaders().getLocation().toString();
        assertThat(location)
                .startsWith("http://localhost:9090/realms/security-baseline/protocol/openid-connect/registrations")
                .doesNotContain("prompt=create")
                .doesNotContain("kc_action=register");
        Object flowIntent = exchange.getSession().block().getAttribute(OidcFlowStateService.FLOW_INTENT_ATTRIBUTE);
        assertThat((String) flowIntent).isEqualTo(OidcFlowIntent.REGISTER.name());
    }

    @Test
    void login_flow_can_force_a_prompt_from_the_bff() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/oauth2/authorization/keycloak").queryParam("prompt", "login").build());
        OidcAuthorizationFlowService service = new OidcAuthorizationFlowService(clientRegistrations(),
                authorizationRequestRepository(), new OidcFlowStateService());

        service.beginLogin(exchange).block();

        assertThat(exchange.getResponse().getHeaders().getLocation().toString()).contains("prompt=login");
    }

    private static ReactiveClientRegistrationRepository clientRegistrations() {
        return new InMemoryReactiveClientRegistrationRepository(ClientRegistration.withRegistrationId("keycloak")
                .clientId("security-baseline-bff")
                .clientSecret("secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .issuerUri("http://localhost:9090/realms/security-baseline")
                .authorizationUri("http://localhost:9090/realms/security-baseline/protocol/openid-connect/auth")
                .tokenUri("http://localhost:9090/realms/security-baseline/protocol/openid-connect/token")
                .jwkSetUri("http://localhost:9090/realms/security-baseline/protocol/openid-connect/certs")
                .userInfoUri("http://localhost:9090/realms/security-baseline/protocol/openid-connect/userinfo")
                .userNameAttributeName("preferred_username")
                .build());
    }

    private static ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository() {
        return new WebSessionOAuth2ServerAuthorizationRequestRepository();
    }
}
