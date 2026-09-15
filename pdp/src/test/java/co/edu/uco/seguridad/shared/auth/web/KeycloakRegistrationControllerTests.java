package co.edu.uco.seguridad.shared.auth.web;

import co.edu.uco.seguridad.shared.auth.service.OidcAuthorizationFlowService;
import co.edu.uco.seguridad.shared.auth.service.OidcFlowStateService;
import co.edu.uco.seguridad.shared.auth.service.OidcReturnTargetPolicy;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.WebSessionOAuth2ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRegistrationControllerTests {

    @Test
    void delegates_to_the_authorization_flow_service_in_registration_mode() {
        var controller = new KeycloakRegistrationController(new OidcAuthorizationFlowService(
                clientRegistrations(), new WebSessionOAuth2ServerAuthorizationRequestRepository(),
                new OidcFlowStateService(), new OidcReturnTargetPolicy(List.of())));
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/oauth2/authorization/keycloak/register").build());

        controller.beginRegistration(exchange).block();

        assertThat(exchange.getResponse().getStatusCode()).hasToString("302 FOUND");
        assertThat(exchange.getResponse().getHeaders().getLocation().toString())
                .startsWith("http://localhost:9090/realms/security-baseline/protocol/openid-connect/registrations");
        Object flowIntent = exchange.getSession().block().getAttribute(OidcFlowStateService.FLOW_INTENT_ATTRIBUTE);
        assertThat((String) flowIntent).isEqualTo("REGISTER");
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
}
