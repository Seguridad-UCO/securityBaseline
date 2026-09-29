package co.edu.uco.seguridad.shared.auth.service;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.request.ProvisionIdentityRequest;
import co.edu.uco.seguridad.pdp.identity.application.usecase.ProvisionIdentityUseCase;
import co.edu.uco.seguridad.shared.auth.model.OidcFlowIntent;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class OidcAuthenticationSuccessHandlerTests {

    @Test
    void login_flow_provisions_the_local_user_and_redirects_to_the_frontend() {
        AtomicReference<String> provider = new AtomicReference<>();
        ProvisionIdentityUseCase provisionIdentity = provisioner(provider);
        OidcFlowStateService flowState = new OidcFlowStateService();
        OidcAuthenticationSuccessHandler handler = new OidcAuthenticationSuccessHandler(provisionIdentity, flowState,
                new OidcRedirectPolicy("http://localhost:5173"),
                new KeycloakOidcSessionService(clientRegistrations()));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/login/oauth2/code/keycloak").build());
        exchange.getSession().block().getAttributes().put(OidcFlowStateService.FLOW_INTENT_ATTRIBUTE, OidcFlowIntent.LOGIN.name());

        handler.onAuthenticationSuccess(webFilterExchange(exchange), new TestingAuthenticationToken(oidcUser(), null)).block();

        assertThat(provider.get()).isEqualTo("google");
        assertThat(exchange.getResponse().getStatusCode()).hasToString("302 FOUND");
        assertThat(exchange.getResponse().getHeaders().getLocation()).hasToString("http://localhost:5173");
        assertThat((Object) exchange.getSession().block().getAttribute(OidcFlowStateService.FLOW_INTENT_ATTRIBUTE)).isNull();
        assertThat((Object) exchange.getSession().block().getAttribute("SPRING_SECURITY_CONTEXT")).isNotNull();
    }

    @Test
    void login_flow_returns_to_the_allowed_application_saved_in_the_flow() {
        OidcFlowStateService flowState = new OidcFlowStateService();
        OidcAuthenticationSuccessHandler handler = new OidcAuthenticationSuccessHandler(provisioner(new AtomicReference<>()),
                flowState, new OidcRedirectPolicy("http://localhost:5173"), new KeycloakOidcSessionService(clientRegistrations()));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/login/oauth2/code/keycloak").build());
        var session = exchange.getSession().block();
        session.getAttributes().put(OidcFlowStateService.FLOW_INTENT_ATTRIBUTE, OidcFlowIntent.LOGIN.name());
        session.getAttributes().put(OidcFlowStateService.RETURN_TARGET_ATTRIBUTE, "http://localhost:5174/grades");

        handler.onAuthenticationSuccess(webFilterExchange(exchange), new TestingAuthenticationToken(oidcUser(), null)).block();

        assertThat(exchange.getResponse().getHeaders().getLocation()).hasToString("http://localhost:5174/grades");
        assertThat((Object) session.getAttribute(OidcFlowStateService.RETURN_TARGET_ATTRIBUTE)).isNull();
    }

    @Test
    void registration_flow_invalidates_the_session_and_restarts_at_login() {
        OidcFlowStateService flowState = new OidcFlowStateService();
        OidcAuthenticationSuccessHandler handler = new OidcAuthenticationSuccessHandler(provisioner(new AtomicReference<>()),
                flowState, new OidcRedirectPolicy("http://localhost:5173"),
                new KeycloakOidcSessionService(clientRegistrations()));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/login/oauth2/code/keycloak").build());
        var session = exchange.getSession().block();
        session.getAttributes().put(OidcFlowStateService.FLOW_INTENT_ATTRIBUTE, OidcFlowIntent.REGISTER.name());

        handler.onAuthenticationSuccess(webFilterExchange(exchange), new TestingAuthenticationToken(oidcUser(), null)).block();

        assertThat(session.isExpired()).isTrue();
        assertThat(exchange.getResponse().getHeaders().getLocation()).hasToString(
                "http://localhost:9090/realms/security-baseline/protocol/openid-connect/logout?client_id=security-baseline-bff&post_logout_redirect_uri=http://localhost:5173?registered%3Dsuccess&id_token_hint=id-token");
    }

    @Test
    void login_flow_persists_the_authentication_context_evidence_extracted_from_the_id_token() {
        OidcFlowStateService flowState = new OidcFlowStateService();
        OidcAuthenticationSuccessHandler handler = new OidcAuthenticationSuccessHandler(provisioner(new AtomicReference<>()),
                flowState, new OidcRedirectPolicy("http://localhost:5173"), new KeycloakOidcSessionService(clientRegistrations()));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/login/oauth2/code/keycloak").build());
        exchange.getSession().block().getAttributes().put(OidcFlowStateService.FLOW_INTENT_ATTRIBUTE, OidcFlowIntent.LOGIN.name());

        handler.onAuthenticationSuccess(webFilterExchange(exchange), new TestingAuthenticationToken(
                oidcUserWithAuthenticationContext("urn:mfa:otp", List.of("otp", "pwd")), null)).block();

        var securityContext = (SecurityContext) exchange.getSession().block().getAttribute("SPRING_SECURITY_CONTEXT");
        var persistedUser = (LocalUserPrincipal) securityContext.getAuthentication().getPrincipal();
        assertThat(persistedUser.authenticationContext().acr()).contains("urn:mfa:otp");
        assertThat(persistedUser.authenticationContext().amr()).containsExactly("otp", "pwd");
    }

    private static ProvisionIdentityUseCase provisioner(AtomicReference<String> provider) {
        return (ProvisionIdentityRequest dto) -> {
            provider.set(dto.provider());
            return Mono.just(new LocalUserPrincipal("user-1", dto.subject(), new TenantId("universidad-uco"),
                    dto.email().value(), dto.name()));
        };
    }

    private static WebFilterExchange webFilterExchange(MockServerWebExchange exchange) {
        WebFilterChain chain = currentExchange -> Mono.empty();
        return new WebFilterExchange(exchange, chain);
    }

    private static OidcUser oidcUser() {
        OidcIdToken idToken = new OidcIdToken("id-token", Instant.now(), Instant.now().plusSeconds(300), Map.of(
                IdTokenClaimNames.ISS, "http://localhost:9090/realms/security-baseline",
                IdTokenClaimNames.SUB, "subject-1",
                "identity_provider", "google",
                StandardClaimNames.EMAIL, "david@uco.edu",
                StandardClaimNames.GIVEN_NAME, "David",
                StandardClaimNames.NAME, "David Alzate"));
        return new DefaultOidcUser(List.of(new OidcUserAuthority(idToken, new OidcUserInfo(Map.of(
                StandardClaimNames.EMAIL, "david@uco.edu",
                StandardClaimNames.NAME, "David Alzate",
                StandardClaimNames.GIVEN_NAME, "David")))), idToken, StandardClaimNames.SUB);
    }

    private static OidcUser oidcUserWithAuthenticationContext(String acr, List<String> amr) {
        OidcIdToken idToken = new OidcIdToken("id-token", Instant.now(), Instant.now().plusSeconds(300), Map.of(
                IdTokenClaimNames.ISS, "http://localhost:9090/realms/security-baseline",
                IdTokenClaimNames.SUB, "subject-1",
                "identity_provider", "google",
                StandardClaimNames.EMAIL, "david@uco.edu",
                StandardClaimNames.GIVEN_NAME, "David",
                StandardClaimNames.NAME, "David Alzate",
                "acr", acr,
                "amr", amr));
        return new DefaultOidcUser(List.of(new OidcUserAuthority(idToken, new OidcUserInfo(Map.of(
                StandardClaimNames.EMAIL, "david@uco.edu",
                StandardClaimNames.NAME, "David Alzate",
                StandardClaimNames.GIVEN_NAME, "David")))), idToken, StandardClaimNames.SUB);
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
                .providerConfigurationMetadata(Map.of(
                        "end_session_endpoint", "http://localhost:9090/realms/security-baseline/protocol/openid-connect/logout"))
                .build();
        return new InMemoryReactiveClientRegistrationRepository(registration);
    }
}
