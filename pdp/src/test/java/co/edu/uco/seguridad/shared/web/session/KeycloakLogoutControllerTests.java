package co.edu.uco.seguridad.shared.web.session;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.auth.service.KeycloakOidcSessionService;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import co.edu.uco.seguridad.shared.security.mfa.AuthenticationContextEvidence;
import co.edu.uco.seguridad.shared.security.revocation.TokenRevocationPort;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakLogoutControllerTests {

    private static final Instant FIXED_NOW = Instant.parse("2026-10-21T12:00:00Z");
    private static final TimeProvider FIXED_TIME = () -> FIXED_NOW;

    @Test
    void revokes_invalidates_the_session_and_redirects_to_keycloak_logout_without_any_refresh_store() {
        var revokedUser = new AtomicReference<UserId>();
        var revokedInstant = new AtomicReference<Instant>();
        TokenRevocationPort revocationPort = new TokenRevocationPort() {
            @Override
            public Mono<Void> revokeAllSince(UserId subject, Instant since) {
                revokedUser.set(subject);
                revokedInstant.set(since);
                return Mono.empty();
            }

            @Override
            public Mono<Void> revokeToken(UserId subject, String tokenId, Instant expiresAt) {
                return Mono.empty();
            }

            @Override
            public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
                return Mono.just(false);
            }

            @Override
            public Mono<Boolean> isTokenRevoked(UserId subject, String tokenId) {
                return Mono.just(false);
            }

            @Override
            public Mono<Void> revokeClientSince(UserId subject, String clientId, Instant since) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Boolean> isClientRevoked(UserId subject, String clientId, Instant issuedAt) {
                throw new UnsupportedOperationException();
            }
        };

        var controller = new KeycloakLogoutController(
                new KeycloakOidcSessionService(clientRegistrations()),
                revocationPort,
                FIXED_TIME,
                "http://localhost:5173");

        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/session/logout").build());
        var session = exchange.getSession().block();
        assertThat(session).isNotNull();
        session.getAttributes().put(KeycloakLogoutController.KEYCLOAK_ID_TOKEN_ATTRIBUTE, "id-token-value");
        session.getAttributes().put(KeycloakLogoutController.KEYCLOAK_SESSION_ID_ATTRIBUTE, "keycloak-session-1");

        UUID uuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UserId userId = new UserId(uuid);
        var localPrincipal = new LocalUserPrincipal(uuid.toString(), "test-subject", new TenantId("tenant-uco"),
                "test@uco.edu.co", "Test User",
                AuthenticationContextEvidence.NONE);
        var securityContext = new SecurityContextImpl(new UsernamePasswordAuthenticationToken(localPrincipal, null, List.of()));
        session.getAttributes().put(new WebSessionServerSecurityContextRepository().getClass().getName() + ".SPRING_SECURITY_CONTEXT", securityContext);

        StepVerifier.create(controller.logout(exchange))
                .verifyComplete();

        assertThat(revokedUser.get()).isEqualTo(userId);
        assertThat(revokedInstant.get()).isEqualTo(FIXED_NOW);
        assertThat(session.isExpired()).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).hasToString("302 FOUND");
        assertThat(exchange.getResponse().getHeaders().getLocation()).hasToString(
                "http://localhost:9090/realms/security-baseline/protocol/openid-connect/logout?client_id=security-baseline-bff&post_logout_redirect_uri=http://localhost:5173&id_token_hint=id-token-value");
    }

    @Test
    void redirects_to_keycloak_without_revocation_when_session_is_anonymous() {
        TokenRevocationPort unreachableRevocation = new TokenRevocationPort() {
            @Override
            public Mono<Void> revokeAllSince(UserId subject, Instant since) {
                return Mono.error(new AssertionError("must not revoke token on anonymous session"));
            }

            @Override
            public Mono<Void> revokeToken(UserId subject, String tokenId, Instant expiresAt) {
                return Mono.error(new AssertionError("must not revoke token on anonymous session"));
            }

            @Override
            public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
                return Mono.just(false);
            }

            @Override
            public Mono<Boolean> isTokenRevoked(UserId subject, String tokenId) {
                return Mono.just(false);
            }

            @Override
            public Mono<Void> revokeClientSince(UserId subject, String clientId, Instant since) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Boolean> isClientRevoked(UserId subject, String clientId, Instant issuedAt) {
                throw new UnsupportedOperationException();
            }
        };

        var controller = new KeycloakLogoutController(
                new KeycloakOidcSessionService(clientRegistrations()),
                unreachableRevocation,
                FIXED_TIME,
                "http://localhost:5173");

        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/session/logout").build());
        var session = exchange.getSession().block();
        assertThat(session).isNotNull();

        StepVerifier.create(controller.logout(exchange))
                .verifyComplete();

        assertThat(session.isExpired()).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).hasToString("302 FOUND");
        assertThat(exchange.getResponse().getHeaders().getLocation()).hasToString(
                "http://localhost:9090/realms/security-baseline/protocol/openid-connect/logout?client_id=security-baseline-bff&post_logout_redirect_uri=http://localhost:5173");
    }

    @Test
    void fails_and_does_not_invalidate_session_when_redis_fails() {
        TokenRevocationPort failingRevocation = new TokenRevocationPort() {
            @Override
            public Mono<Void> revokeAllSince(UserId subject, Instant since) {
                return Mono.error(new RedisSystemException("Redis down", new RuntimeException("connection refused")));
            }

            @Override
            public Mono<Void> revokeToken(UserId subject, String tokenId, Instant expiresAt) {
                return Mono.error(new UnsupportedOperationException());
            }

            @Override
            public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
                return Mono.just(false);
            }

            @Override
            public Mono<Boolean> isTokenRevoked(UserId subject, String tokenId) {
                return Mono.just(false);
            }

            @Override
            public Mono<Void> revokeClientSince(UserId subject, String clientId, Instant since) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Boolean> isClientRevoked(UserId subject, String clientId, Instant issuedAt) {
                throw new UnsupportedOperationException();
            }
        };

        var controller = new KeycloakLogoutController(
                new KeycloakOidcSessionService(clientRegistrations()),
                failingRevocation,
                FIXED_TIME,
                "http://localhost:5173");

        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/session/logout").build());
        var session = exchange.getSession().block();
        assertThat(session).isNotNull();

        UUID uuid = UUID.fromString("22222222-2222-2222-2222-222222222222");
        var localPrincipal = new LocalUserPrincipal(uuid.toString(), "test-sub", new TenantId("tenant-uco"),
                "test@uco.edu.co", "User",
                AuthenticationContextEvidence.NONE);
        var securityContext = new SecurityContextImpl(new UsernamePasswordAuthenticationToken(localPrincipal, null, List.of()));
        session.getAttributes().put(new WebSessionServerSecurityContextRepository().getClass().getName() + ".SPRING_SECURITY_CONTEXT", securityContext);

        StepVerifier.create(controller.logout(exchange))
                .expectError(RedisSystemException.class)
                .verify();

        assertThat(session.isExpired()).isFalse();
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
