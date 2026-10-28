package co.edu.uco.seguridad.shared.web.session;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.shared.auth.model.TokenRefreshResult;
import co.edu.uco.seguridad.shared.auth.port.TokenRefreshPort;
import co.edu.uco.seguridad.shared.security.revocation.TokenRevocationPort;
import co.edu.uco.seguridad.shared.web.session.dto.TokenRefreshResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TokenRefreshControllerTests {

    private static final Instant FIXED_NOW = Instant.parse("2026-10-21T16:00:00Z");
    private static final UserId USER_ID = new UserId(UUID.fromString("66666666-6666-6666-6666-666666666666"));

    @Test
    void refreshes_token_and_revokes_only_the_previous_jti_when_token_is_valid() {
        var revokedSubject = new AtomicReference<UserId>();
        var revokedTokenId = new AtomicReference<String>();
        var revokedExpiry = new AtomicReference<Instant>();
        var refreshedSession = new AtomicReference<String>();

        TokenRefreshPort refreshPort = sessionId -> {
            refreshedSession.set(sessionId);
            return Mono.just(new TokenRefreshResult("new-jwt-token-value", 300L, "Bearer"));
        };
        TokenRevocationPort revocationPort = new TokenRevocationPort() {
            @Override
            public Mono<Void> revokeAllSince(UserId subject, Instant since) {
                return Mono.error(new AssertionError("refresh must not revoke other tokens for the subject"));
            }

            @Override
            public Mono<Void> revokeToken(UserId subject, String tokenId, Instant expiresAt) {
                revokedSubject.set(subject);
                revokedTokenId.set(tokenId);
                revokedExpiry.set(expiresAt);
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
        };
        SubjectUserIdLookupValidator subjectLookup = subject -> Mono.just(USER_ID);
        var controller = new TokenRefreshController(refreshPort, revocationPort, subjectLookup);
        var auth = new UsernamePasswordAuthenticationToken(accessToken("jwt-id-prev"), null, List.of());

        StepVerifier.create(controller.refresh()
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
                .assertNext(response -> {
                    assertThat(response).isEqualTo(new TokenRefreshResponse("new-jwt-token-value", 300L, "Bearer"));
                    assertThat(response.accessToken()).isEqualTo("new-jwt-token-value");
                    assertThat(response.expiresIn()).isEqualTo(300L);
                    assertThat(response.tokenType()).isEqualTo("Bearer");
                })
                .verifyComplete();

        assertThat(refreshedSession.get()).isEqualTo("keycloak-session-1");
        assertThat(revokedSubject.get()).isEqualTo(USER_ID);
        assertThat(revokedTokenId.get()).isEqualTo("jwt-id-prev");
        assertThat(revokedExpiry.get()).isEqualTo(FIXED_NOW.plusSeconds(300));
    }

    @Test
    void propagates_error_without_revoking_previous_token_when_keycloak_fails() {
        TokenRefreshPort failingRefreshPort = sessionId ->
                Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Keycloak unavailable"));
        TokenRevocationPort unreachableRevocation = new TokenRevocationPort() {
            @Override
            public Mono<Void> revokeAllSince(UserId subject, Instant since) {
                return Mono.error(new AssertionError("must not revoke token if refresh failed"));
            }

            @Override
            public Mono<Void> revokeToken(UserId subject, String tokenId, Instant expiresAt) {
                return Mono.error(new AssertionError("must not revoke token if refresh failed"));
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
        var controller = new TokenRefreshController(failingRefreshPort, unreachableRevocation,
                subject -> Mono.just(USER_ID));
        var auth = new UsernamePasswordAuthenticationToken(accessToken("jwt-id-prev"), null, List.of());

        StepVerifier.create(controller.refresh()
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
                .expectErrorMatches(error -> error instanceof ResponseStatusException rse
                        && rse.getStatusCode().equals(HttpStatus.SERVICE_UNAVAILABLE))
                .verify();
    }

    @Test
    void fails_with_session_expired_when_idp_session_is_expired() {
        TokenRefreshPort expiredRefreshPort = sessionId ->
                Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED"));
        TokenRevocationPort unreachableRevocation = new TokenRevocationPort() {
            @Override
            public Mono<Void> revokeAllSince(UserId subject, Instant since) {
                return Mono.error(new AssertionError("must not revoke token if session expired"));
            }

            @Override
            public Mono<Void> revokeToken(UserId subject, String tokenId, Instant expiresAt) {
                return Mono.error(new AssertionError("must not revoke token if session expired"));
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
        var controller = new TokenRefreshController(expiredRefreshPort, unreachableRevocation,
                subject -> Mono.just(USER_ID));
        var auth = new UsernamePasswordAuthenticationToken(accessToken("jwt-id-prev"), null, List.of());

        StepVerifier.create(controller.refresh()
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
                .expectErrorMatches(error -> error instanceof ResponseStatusException rse
                        && rse.getStatusCode().equals(HttpStatus.UNAUTHORIZED))
                .verify();
    }

    private static Jwt accessToken(String tokenId) {
        return Jwt.withTokenValue("access-token")
                .header("alg", "RS256")
                .subject("subject-refresh")
                .claim("jti", tokenId)
                .issuedAt(FIXED_NOW.minusSeconds(10))
                .expiresAt(FIXED_NOW.plusSeconds(300))
                .claim("sid", "keycloak-session-1")
                .build();
    }
}
