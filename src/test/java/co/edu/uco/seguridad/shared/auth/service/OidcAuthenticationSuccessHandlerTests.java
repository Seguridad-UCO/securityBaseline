package co.edu.uco.seguridad.shared.auth.service;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.platform.application.PlatformAdministrationService;
import co.edu.uco.seguridad.shared.auth.model.OidcFlowIntent;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
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
        PlatformAdministrationService users = provisioner(provider);
        OidcFlowStateService flowState = new OidcFlowStateService();
        OidcAuthenticationSuccessHandler handler = new OidcAuthenticationSuccessHandler(users, flowState,
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

    private static PlatformAdministrationService provisioner(AtomicReference<String> provider) {
        return new PlatformAdministrationService() {
            @Override
            public Mono<LocalUserPrincipal> provision(String issuer, String subject, String email, String name, String authProvider) {
                provider.set(authProvider);
                return Mono.just(new LocalUserPrincipal("user-1", subject, new TenantId("universidad-uco"), email, name));
            }

            @Override public Mono<List<ApplicationView>> applications(String tenantId) { return Mono.empty(); }
            @Override public Mono<ApplicationView> createApplication(String tenantId, String name, String description, String baseUrl) { return Mono.empty(); }
            @Override public Mono<List<ResourceView>> resources(String tenantId, String applicationId) { return Mono.empty(); }
            @Override public Mono<ResourceView> createResource(String tenantId, String applicationId, String path, String method) { return Mono.empty(); }
            @Override public Mono<List<TenantView>> tenants() { return Mono.empty(); }
            @Override public Mono<TenantView> createTenant(String code, String name) { return Mono.empty(); }
            @Override public Mono<List<UserView>> users() { return Mono.empty(); }
            @Override public Mono<UserView> assignTenant(String userId, String tenantCode) { return Mono.empty(); }
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
