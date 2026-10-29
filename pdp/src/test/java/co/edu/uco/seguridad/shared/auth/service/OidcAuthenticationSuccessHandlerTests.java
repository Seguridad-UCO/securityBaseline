package co.edu.uco.seguridad.shared.auth.service;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.request.ProvisionIdentityRequest;
import co.edu.uco.seguridad.pdp.identity.application.usecase.ProvisionIdentityUseCase;
import co.edu.uco.seguridad.shared.auth.port.RefreshTokenStorePort;
import co.edu.uco.seguridad.shared.auth.model.OidcFlowIntent;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
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
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OidcAuthenticationSuccessHandlerTests {

    @Test
    void login_flow_provisions_the_local_user_and_redirects_to_the_frontend() {
        AtomicReference<String> provider = new AtomicReference<>();
        AtomicReference<String> storedSessionId = new AtomicReference<>();
        ProvisionIdentityUseCase provisionIdentity = provisioner(provider);
        OidcFlowStateService flowState = new OidcFlowStateService();
        OidcAuthenticationSuccessHandler handler = new OidcAuthenticationSuccessHandler(provisionIdentity, flowState,
                new OidcRedirectPolicy("http://localhost:5173"),
                new KeycloakOidcSessionService(clientRegistrations()), authorizedClients(),
                refreshTokenStore(storedSessionId), expirationTracker());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/login/oauth2/code/keycloak").build());
        exchange.getSession().block().getAttributes().put(OidcFlowStateService.FLOW_INTENT_ATTRIBUTE, OidcFlowIntent.LOGIN.name());

        handler.onAuthenticationSuccess(webFilterExchange(exchange), new TestingAuthenticationToken(oidcUser(), null)).block();

        assertThat(provider.get()).isEqualTo("google");
        assertThat(exchange.getResponse().getStatusCode()).hasToString("302 FOUND");
        assertThat(exchange.getResponse().getHeaders().getLocation()).hasToString("http://localhost:5173");
        assertThat((Object) exchange.getSession().block().getAttribute(OidcFlowStateService.FLOW_INTENT_ATTRIBUTE)).isNull();
        assertThat((Object) exchange.getSession().block().getAttribute("SPRING_SECURITY_CONTEXT")).isNotNull();
        assertThat(storedSessionId.get()).isEqualTo("keycloak-session-1");
    }

    @Test
    void login_flow_returns_to_the_allowed_application_saved_in_the_flow() {
        OidcFlowStateService flowState = new OidcFlowStateService();
        OidcAuthenticationSuccessHandler handler = new OidcAuthenticationSuccessHandler(provisioner(new AtomicReference<>()),
                flowState, new OidcRedirectPolicy("http://localhost:5173"), new KeycloakOidcSessionService(clientRegistrations()),
                authorizedClients(), refreshTokenStore(), expirationTracker());
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
                new KeycloakOidcSessionService(clientRegistrations()), authorizedClients(), refreshTokenStore(),
                expirationTracker());
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
                flowState, new OidcRedirectPolicy("http://localhost:5173"), new KeycloakOidcSessionService(clientRegistrations()),
                authorizedClients(), refreshTokenStore(), expirationTracker());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/login/oauth2/code/keycloak").build());
        exchange.getSession().block().getAttributes().put(OidcFlowStateService.FLOW_INTENT_ATTRIBUTE, OidcFlowIntent.LOGIN.name());

        handler.onAuthenticationSuccess(webFilterExchange(exchange), new TestingAuthenticationToken(
                oidcUserWithAuthenticationContext("urn:mfa:otp", List.of("otp", "pwd")), null)).block();

        var securityContext = (SecurityContext) exchange.getSession().block().getAttribute("SPRING_SECURITY_CONTEXT");
        var persistedUser = (LocalUserPrincipal) securityContext.getAuthentication().getPrincipal();
        assertThat(persistedUser.authenticationContext().acr()).contains("urn:mfa:otp");
        assertThat(persistedUser.authenticationContext().amr()).containsExactly("otp", "pwd");
    }

    @Test
    void login_flow_uses_keycloak_refresh_expiration_when_spring_does_not_map_it() {
        AtomicReference<Instant> storedExpiration = new AtomicReference<>();
        KeycloakRefreshTokenExpirationTracker expirationTracker = expirationTracker();
        OAuth2AccessTokenResponse tokenResponse = OAuth2AccessTokenResponse.withToken("access-token")
                .tokenType(OAuth2AccessToken.TokenType.BEARER)
                .expiresIn(300)
                .refreshToken("refresh-token")
                .additionalParameters(Map.of("refresh_expires_in", 1800))
                .build();
        expirationTracker.record(tokenResponse);

        OidcFlowStateService flowState = new OidcFlowStateService();
        OidcAuthenticationSuccessHandler handler = new OidcAuthenticationSuccessHandler(
                provisioner(new AtomicReference<>()), flowState, new OidcRedirectPolicy("http://localhost:5173"),
                new KeycloakOidcSessionService(clientRegistrations()), authorizedClientsWithoutRefreshExpiration(),
                refreshTokenStore(new AtomicReference<>(), storedExpiration), expirationTracker);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/login/oauth2/code/keycloak").build());
        exchange.getSession().block().getAttributes()
                .put(OidcFlowStateService.FLOW_INTENT_ATTRIBUTE, OidcFlowIntent.LOGIN.name());

        handler.onAuthenticationSuccess(webFilterExchange(exchange), new TestingAuthenticationToken(oidcUser(), null))
                .block();

        assertThat(storedExpiration.get()).isEqualTo(
                tokenResponse.getRefreshToken().getIssuedAt().plusSeconds(1800));
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
                "sid", "keycloak-session-1",
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
                "sid", "keycloak-session-1",
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
        return new InMemoryReactiveClientRegistrationRepository(clientRegistration());
    }

    private static ClientRegistration clientRegistration() {
        return ClientRegistration.withRegistrationId("keycloak")
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
    }

    private static ServerOAuth2AuthorizedClientRepository authorizedClients() {
        ServerOAuth2AuthorizedClientRepository repository = mock(ServerOAuth2AuthorizedClientRepository.class);
        Instant issuedAt = Instant.now();
        OAuth2AuthorizedClient client = new OAuth2AuthorizedClient(clientRegistration(), "subject-1",
                new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access-token", issuedAt,
                        issuedAt.plusSeconds(300), Set.of("openid")),
                new OAuth2RefreshToken("refresh-token", issuedAt, issuedAt.plusSeconds(1800)));
        when(repository.loadAuthorizedClient(eq("keycloak"), any(), any(ServerWebExchange.class)))
                .thenReturn(Mono.just(client));
        return repository;
    }

    private static ServerOAuth2AuthorizedClientRepository authorizedClientsWithoutRefreshExpiration() {
        ServerOAuth2AuthorizedClientRepository repository = mock(ServerOAuth2AuthorizedClientRepository.class);
        Instant issuedAt = Instant.now();
        OAuth2AuthorizedClient client = new OAuth2AuthorizedClient(clientRegistration(), "subject-1",
                new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access-token", issuedAt,
                        issuedAt.plusSeconds(300), Set.of("openid")),
                new OAuth2RefreshToken("refresh-token", issuedAt));
        when(repository.loadAuthorizedClient(eq("keycloak"), any(), any(ServerWebExchange.class)))
                .thenReturn(Mono.just(client));
        return repository;
    }

    private static KeycloakRefreshTokenExpirationTracker expirationTracker() {
        return new KeycloakRefreshTokenExpirationTracker(Instant::now);
    }

    private static RefreshTokenStorePort refreshTokenStore() {
        return refreshTokenStore(new AtomicReference<>());
    }

    private static RefreshTokenStorePort refreshTokenStore(AtomicReference<String> storedSessionId) {
        return refreshTokenStore(storedSessionId, new AtomicReference<>());
    }

    private static RefreshTokenStorePort refreshTokenStore(AtomicReference<String> storedSessionId,
                                                           AtomicReference<Instant> storedExpiration) {
        return new RefreshTokenStorePort() {
            @Override
            public Mono<Void> store(String sessionId, String refreshToken, Instant expiresAt) {
                storedSessionId.set(sessionId);
                storedExpiration.set(expiresAt);
                return Mono.empty();
            }

            @Override
            public Mono<String> find(String sessionId) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> delete(String sessionId) {
                return Mono.empty();
            }
        };
    }
}
