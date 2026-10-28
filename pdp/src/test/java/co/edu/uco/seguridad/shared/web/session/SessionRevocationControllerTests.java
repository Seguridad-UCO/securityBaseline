package co.edu.uco.seguridad.shared.web.session;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.shared.auth.port.RefreshTokenStorePort;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.security.mfa.AuthenticationContextEvidence;
import co.edu.uco.seguridad.shared.security.revocation.RevocationAwareJwtDecoder;
import co.edu.uco.seguridad.shared.security.revocation.TokenRevocationPort;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SessionRevocationControllerTests {

    private static final Instant FIXED_NOW = Instant.parse("2026-10-21T14:30:00Z");
    private static final TimeProvider FIXED_TIME = () -> FIXED_NOW;
    private static final UserId USER_ID = new UserId(UUID.fromString("33333333-3333-3333-3333-333333333333"));

    @Test
    void revokes_all_tokens_since_now_when_authenticated_with_pdp_principal() {
        var recordedSubject = new AtomicReference<UserId>();
        var recordedInstant = new AtomicReference<Instant>();
        TokenRevocationPort revocationPort = revocationPort(recordedSubject, recordedInstant);
        var controller = controller(revocationPort, unusedRefreshTokenStore());
        var principal = new PdpPrincipal(new TenantId("tenant-uco"), "subject-pdp", "jwt-id-123",
                Optional.of(USER_ID), AuthenticationContextEvidence.NONE);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.delete("/api/v1/session").build());

        StepVerifier.create(controller.revokeSession(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
                .verifyComplete();

        assertThat(recordedSubject.get()).isEqualTo(USER_ID);
        assertThat(recordedInstant.get()).isEqualTo(FIXED_NOW);
    }

    @Test
    void revokes_tokens_and_removes_refresh_credential_when_authenticated_with_keycloak_bearer() {
        var recordedSubject = new AtomicReference<UserId>();
        var recordedInstant = new AtomicReference<Instant>();
        var deletedSessionId = new AtomicReference<String>();
        TokenRevocationPort revocationPort = revocationPort(recordedSubject, recordedInstant);
        RefreshTokenStorePort store = new RefreshTokenStorePort() {
            @Override
            public Mono<Void> store(String sessionId, String refreshToken, Instant expiresAt) {
                return Mono.error(new UnsupportedOperationException());
            }

            @Override
            public Mono<String> find(String sessionId) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> delete(String sessionId) {
                deletedSessionId.set(sessionId);
                return Mono.empty();
            }
        };
        var controller = controller(revocationPort, store);
        Jwt jwt = keycloakToken("jti-logout");
        var auth = new UsernamePasswordAuthenticationToken(jwt, null, List.of());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.delete("/api/v1/session").build());

        StepVerifier.create(controller.revokeSession(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
                .verifyComplete();

        assertThat(recordedSubject.get()).isEqualTo(USER_ID);
        assertThat(recordedInstant.get()).isEqualTo(FIXED_NOW);
        assertThat(deletedSessionId.get()).isEqualTo("keycloak-session-1");
    }

    @Test
    void rejects_the_same_bearer_after_successful_logout() {
        var revokedAt = new AtomicReference<Instant>();
        var revokedTokenId = new AtomicReference<String>();
        TokenRevocationPort revocationPort = new TokenRevocationPort() {
            @Override
            public Mono<Void> revokeAllSince(UserId subject, Instant since) {
                revokedAt.set(since);
                return Mono.empty();
            }

            @Override
            public Mono<Void> revokeToken(UserId subject, String tokenId, Instant expiresAt) {
                revokedTokenId.set(tokenId);
                return Mono.empty();
            }

            @Override
            public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
                return Mono.just(revokedAt.get() != null && !issuedAt.isAfter(revokedAt.get()));
            }

            @Override
            public Mono<Boolean> isTokenRevoked(UserId subject, String tokenId) {
                return Mono.just(tokenId.equals(revokedTokenId.get()));
            }
        };
        var deletedSessionId = new AtomicReference<String>();
        var controller = controller(revocationPort, storeDeleting(deletedSessionId));
        Jwt jwt = keycloakToken("jti-logout");
        var auth = new UsernamePasswordAuthenticationToken(jwt, null, List.of());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.delete("/api/v1/session").build());

        StepVerifier.create(controller.revokeSession(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
                .verifyComplete();

        ReactiveJwtDecoder decoder = new RevocationAwareJwtDecoder(token -> Mono.just(jwt),
                revocationPort, subject -> Mono.just(USER_ID));
        StepVerifier.create(decoder.decode("same-token"))
                .expectError(JwtValidationException.class)
                .verify();
        assertThat(deletedSessionId.get()).isEqualTo("keycloak-session-1");
    }

    @Test
    void revokes_all_tokens_when_authenticated_with_local_user_principal() {
        var recordedSubject = new AtomicReference<UserId>();
        var recordedInstant = new AtomicReference<Instant>();
        var deletedSessionId = new AtomicReference<String>();
        var store = storeDeleting(deletedSessionId);
        var controller = controller(revocationPort(recordedSubject, recordedInstant), store);
        UUID uuid = USER_ID.value();
        var principal = new LocalUserPrincipal(uuid.toString(), "sub-local", new TenantId("tenant-uco"),
                "local@uco.edu.co", "Local User");
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.delete("/api/v1/session").build());
        var session = exchange.getSession().block();
        session.getAttributes().put(KeycloakLogoutController.KEYCLOAK_SESSION_ID_ATTRIBUTE, "keycloak-session-2");

        StepVerifier.create(controller.revokeSession(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
                .verifyComplete();

        assertThat(recordedSubject.get()).isEqualTo(USER_ID);
        assertThat(recordedInstant.get()).isEqualTo(FIXED_NOW);
        assertThat(deletedSessionId.get()).isEqualTo("keycloak-session-2");
        assertThat(session.isExpired()).isTrue();
    }

    @Test
    void propagates_error_when_redis_fails() {
        TokenRevocationPort failingRevocation = new TokenRevocationPort() {
            @Override
            public Mono<Void> revokeAllSince(UserId subject, Instant since) {
                return Mono.error(new RedisSystemException("Redis unreachable", new RuntimeException("connection timeout")));
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
        };
        var controller = controller(failingRevocation, unusedRefreshTokenStore());
        var principal = new PdpPrincipal(new TenantId("tenant-uco"), "subject-pdp", "jwt-id-999",
                Optional.of(USER_ID), AuthenticationContextEvidence.NONE);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.delete("/api/v1/session").build());

        StepVerifier.create(controller.revokeSession(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
                .expectError(RedisSystemException.class)
                .verify();
    }

    private static SessionRevocationController controller(TokenRevocationPort revocationPort,
                                                           RefreshTokenStorePort refreshTokenStore) {
        SubjectUserIdLookupValidator lookup = subject -> Mono.just(USER_ID);
        return new SessionRevocationController(revocationPort, FIXED_TIME, lookup, refreshTokenStore);
    }

    private static TokenRevocationPort revocationPort(AtomicReference<UserId> subject, AtomicReference<Instant> instant) {
        return new TokenRevocationPort() {
            @Override
            public Mono<Void> revokeAllSince(UserId userId, Instant since) {
                subject.set(userId);
                instant.set(since);
                return Mono.empty();
            }

            @Override
            public Mono<Void> revokeToken(UserId userId, String tokenId, Instant expiresAt) {
                return Mono.empty();
            }

            @Override
            public Mono<Boolean> isRevoked(UserId userId, Instant issuedAt) {
                return Mono.just(false);
            }

            @Override
            public Mono<Boolean> isTokenRevoked(UserId userId, String tokenId) {
                return Mono.just(false);
            }
        };
    }

    private static RefreshTokenStorePort unusedRefreshTokenStore() {
        return new RefreshTokenStorePort() {
            @Override
            public Mono<Void> store(String sessionId, String refreshToken, Instant expiresAt) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<String> find(String sessionId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Void> delete(String sessionId) {
                throw new AssertionError("no Keycloak session id should be deleted");
            }
        };
    }

    private static RefreshTokenStorePort storeDeleting(AtomicReference<String> deletedSessionId) {
        return new RefreshTokenStorePort() {
            @Override
            public Mono<Void> store(String sessionId, String refreshToken, Instant expiresAt) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<String> find(String sessionId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Void> delete(String sessionId) {
                deletedSessionId.set(sessionId);
                return Mono.empty();
            }
        };
    }

    private static Jwt keycloakToken(String tokenId) {
        return Jwt.withTokenValue("access-token")
                .header("alg", "RS256")
                .subject("keycloak-subject")
                .claim("jti", tokenId)
                .issuedAt(FIXED_NOW.minusSeconds(60))
                .expiresAt(FIXED_NOW.plusSeconds(300))
                .claim("sid", "keycloak-session-1")
                .build();
    }
}
