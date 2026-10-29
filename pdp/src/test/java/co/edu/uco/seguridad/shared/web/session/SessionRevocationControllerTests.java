package co.edu.uco.seguridad.shared.web.session;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.security.mfa.AuthenticationContextEvidence;
import co.edu.uco.seguridad.shared.security.revocation.RevocationAwareJwtDecoder;
import co.edu.uco.seguridad.shared.security.revocation.TokenRevocationPort;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-028 (replanteada, ADR-034): el canal decide el alcance del logout. Cookie BFF = global;
 * bearer = el {@code jti} presentado y los tokens del mismo cliente ({@code azp}).
 */
class SessionRevocationControllerTests {

    private static final Instant FIXED_NOW = Instant.parse("2026-10-21T14:30:00Z");
    private static final TimeProvider FIXED_TIME = () -> FIXED_NOW;
    private static final UserId USER_ID = new UserId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
    private static final String CLIENT_A = "app-a";

    // ---- canal bearer ----

    @Test
    void bearer_with_azp_revokes_the_jti_and_the_client_of_the_subject_but_not_the_whole_user() {
        List<String> calls = new ArrayList<>();
        var controller = controller(revocation(calls, null));
        Jwt jwt = bearerToken("jti-logout", CLIENT_A);

        StepVerifier.create(revokeAs(controller, jwt, newExchange()))
                .verifyComplete();

        assertThat(calls).containsExactly(
                "revokeToken:" + USER_ID.value() + ":jti-logout:" + jwt.getExpiresAt(),
                "revokeClientSince:" + USER_ID.value() + ":" + CLIENT_A + ":" + FIXED_NOW);
    }

    @Test
    void bearer_never_invalidates_the_web_session() {
        List<String> calls = new ArrayList<>();
        var controller = controller(revocation(calls, null));
        MockServerWebExchange exchange = newExchange();
        WebSession session = exchange.getSession().block();
        assertThat(session).isNotNull();
        session.getAttributes().put(KeycloakLogoutController.KEYCLOAK_SESSION_ID_ATTRIBUTE, "keycloak-session-1");

        StepVerifier.create(revokeAs(controller, bearerToken("jti-logout", CLIENT_A), exchange))
                .verifyComplete();

        assertThat(session.isExpired()).isFalse();
        assertThat(calls).isNotEmpty();
    }

    @Test
    void bearer_without_azp_revokes_only_its_own_jti() {
        List<String> calls = new ArrayList<>();
        var controller = controller(revocation(calls, null));
        Jwt jwt = bearerToken("jti-no-azp", null);

        StepVerifier.create(revokeAs(controller, jwt, newExchange()))
                .verifyComplete();

        assertThat(calls).containsExactly(
                "revokeToken:" + USER_ID.value() + ":jti-no-azp:" + jwt.getExpiresAt());
    }

    @Test
    void bearer_with_a_blank_azp_is_treated_as_without_azp() {
        List<String> calls = new ArrayList<>();
        var controller = controller(revocation(calls, null));

        StepVerifier.create(revokeAs(controller, bearerToken("jti-blank-azp", "   "), newExchange()))
                .verifyComplete();

        assertThat(calls).hasSize(1).allMatch(call -> call.startsWith("revokeToken:"));
    }

    @Test
    void bearer_propagates_the_redis_failure_and_leaves_the_web_session_untouched() {
        RedisSystemException redisDown = new RedisSystemException("Redis unreachable", new RuntimeException("timeout"));
        List<String> calls = new ArrayList<>();
        var controller = controller(revocation(calls, redisDown));
        MockServerWebExchange exchange = newExchange();
        WebSession session = exchange.getSession().block();
        assertThat(session).isNotNull();

        StepVerifier.create(revokeAs(controller, bearerToken("jti-logout", CLIENT_A), exchange))
                .expectError(RedisSystemException.class)
                .verify();

        assertThat(session.isExpired()).isFalse();
    }

    @Test
    void bearer_whose_subject_has_no_local_identity_is_rejected_with_401_and_revokes_nothing() {
        List<String> calls = new ArrayList<>();
        SubjectUserIdLookupValidator unknownSubject = subject -> Mono.empty();
        var controller = new SessionRevocationController(revocation(calls, null), FIXED_TIME, unknownSubject);

        StepVerifier.create(revokeAs(controller, bearerToken("jti-logout", CLIENT_A), newExchange()))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ResponseStatusException.class);
                    assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                })
                .verify();

        assertThat(calls).isEmpty();
    }

    @Test
    void the_same_bearer_and_older_tokens_of_its_client_are_rejected_by_the_decoder_after_logout_but_other_clients_are_not() {
        StatefulRevocation state = new StatefulRevocation();
        var controller = controller(state);
        Jwt loggedOut = bearerToken("jti-logout", CLIENT_A);

        StepVerifier.create(revokeAs(controller, loggedOut, newExchange()))
                .verifyComplete();

        StepVerifier.create(decoderReturning(loggedOut, state).decode("same-token"))
                .expectError(JwtValidationException.class)
                .verify();

        Jwt anotherTokenOfClientA = bearerToken("jti-other", CLIENT_A);
        StepVerifier.create(decoderReturning(anotherTokenOfClientA, state).decode("other-token"))
                .expectError(JwtValidationException.class)
                .verify();

        Jwt tokenOfClientB = bearerToken("jti-client-b", "app-b");
        StepVerifier.create(decoderReturning(tokenOfClientB, state).decode("client-b-token"))
                .expectNext(tokenOfClientB)
                .verifyComplete();
    }

    // ---- canal cookie BFF ----

    @Test
    void cookie_session_revokes_all_tokens_of_the_user_and_invalidates_the_web_session_without_any_refresh_store() {
        List<String> calls = new ArrayList<>();
        var controller = controller(revocation(calls, null));
        var principal = new LocalUserPrincipal(USER_ID.value().toString(), "sub-local", new TenantId("tenant-uco"),
                "local@uco.edu.co", "Local User");
        MockServerWebExchange exchange = newExchange();
        WebSession session = exchange.getSession().block();
        assertThat(session).isNotNull();
        session.getAttributes().put(KeycloakLogoutController.KEYCLOAK_SESSION_ID_ATTRIBUTE, "keycloak-session-2");

        StepVerifier.create(controller.revokeSession(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                                new UsernamePasswordAuthenticationToken(principal, null, List.of()))))
                .verifyComplete();

        assertThat(calls).containsExactly(
                "revokeAllSince:" + USER_ID.value() + ":" + FIXED_NOW);
        assertThat(session.isExpired()).isTrue();
    }

    @Test
    void cookie_session_fails_without_invalidating_the_session_when_redis_fails() {
        RedisSystemException redisDown = new RedisSystemException("Redis unreachable", new RuntimeException("timeout"));
        List<String> calls = new ArrayList<>();
        var controller = controller(revocation(calls, redisDown));
        var principal = new LocalUserPrincipal(USER_ID.value().toString(), "sub-local", new TenantId("tenant-uco"),
                "local@uco.edu.co", "Local User");
        MockServerWebExchange exchange = newExchange();
        WebSession session = exchange.getSession().block();
        assertThat(session).isNotNull();
        session.getAttributes().put(KeycloakLogoutController.KEYCLOAK_SESSION_ID_ATTRIBUTE, "keycloak-session-2");

        StepVerifier.create(controller.revokeSession(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                                new UsernamePasswordAuthenticationToken(principal, null, List.of()))))
                .expectError(RedisSystemException.class)
                .verify();

        assertThat(session.isExpired()).isFalse();
    }

    @Test
    void pdp_principal_revokes_all_tokens_of_the_user_and_never_a_single_client() {
        List<String> calls = new ArrayList<>();
        var controller = controller(revocation(calls, null));
        var principal = new PdpPrincipal(new TenantId("tenant-uco"), "subject-pdp", "jwt-id-123",
                Optional.of(USER_ID), AuthenticationContextEvidence.NONE);

        StepVerifier.create(controller.revokeSession(newExchange())
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                                new UsernamePasswordAuthenticationToken(principal, null, List.of()))))
                .verifyComplete();

        assertThat(calls).containsExactly("revokeAllSince:" + USER_ID.value() + ":" + FIXED_NOW);
    }

    // ---- ayudas ----

    private static Mono<Void> revokeAs(SessionRevocationController controller, Jwt jwt, MockServerWebExchange exchange) {
        return controller.revokeSession(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                        new UsernamePasswordAuthenticationToken(jwt, null, List.of())));
    }

    private static MockServerWebExchange newExchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.delete("/api/v1/session").build());
    }

    private static SessionRevocationController controller(TokenRevocationPort revocationPort) {
        SubjectUserIdLookupValidator lookup = subject -> Mono.just(USER_ID);
        return new SessionRevocationController(revocationPort, FIXED_TIME, lookup);
    }

    private static ReactiveJwtDecoder decoderReturning(Jwt jwt, TokenRevocationPort revocation) {
        return new RevocationAwareJwtDecoder(token -> Mono.just(jwt), revocation, subject -> Mono.just(USER_ID));
    }

    /** Registra cada escritura al suscribirse (como el adaptador real, que es frío); si {@code writeFailure} no es nulo, toda escritura falla con él. */
    private static TokenRevocationPort revocation(List<String> calls, RuntimeException writeFailure) {
        return new TokenRevocationPort() {
            @Override
            public Mono<Void> revokeAllSince(UserId subject, Instant since) {
                if (writeFailure != null) {
                    return Mono.error(writeFailure);
                }
                return Mono.fromRunnable(() -> calls.add("revokeAllSince:" + subject.value() + ":" + since));
            }

            @Override
            public Mono<Void> revokeToken(UserId subject, String tokenId, Instant expiresAt) {
                if (writeFailure != null) {
                    return Mono.error(writeFailure);
                }
                return Mono.fromRunnable(() -> calls.add("revokeToken:" + subject.value() + ":" + tokenId + ":" + expiresAt));
            }

            @Override
            public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Boolean> isTokenRevoked(UserId subject, String tokenId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Void> revokeClientSince(UserId subject, String clientId, Instant since) {
                if (writeFailure != null) {
                    return Mono.error(writeFailure);
                }
                return Mono.fromRunnable(() -> calls.add("revokeClientSince:" + subject.value() + ":" + clientId + ":" + since));
            }

            @Override
            public Mono<Boolean> isClientRevoked(UserId subject, String clientId, Instant issuedAt) {
                throw new UnsupportedOperationException();
            }
        };
    }

    /** Doble con estado que replica la semántica del puerto, para cerrar el ciclo logout -> decoder. */
    private static final class StatefulRevocation implements TokenRevocationPort {
        private final Map<String, Instant> revokedTokens = new HashMap<>();
        private final Map<String, Instant> revokedClients = new HashMap<>();
        private Instant revokedAllSince;

        @Override
        public Mono<Void> revokeAllSince(UserId subject, Instant since) {
            revokedAllSince = since;
            return Mono.empty();
        }

        @Override
        public Mono<Void> revokeToken(UserId subject, String tokenId, Instant expiresAt) {
            revokedTokens.put(tokenId, expiresAt);
            return Mono.empty();
        }

        @Override
        public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
            return Mono.just(revokedAllSince != null && !issuedAt.isAfter(revokedAllSince));
        }

        @Override
        public Mono<Boolean> isTokenRevoked(UserId subject, String tokenId) {
            return Mono.just(revokedTokens.containsKey(tokenId));
        }

        @Override
        public Mono<Void> revokeClientSince(UserId subject, String clientId, Instant since) {
            revokedClients.put(clientId, since);
            return Mono.empty();
        }

        @Override
        public Mono<Boolean> isClientRevoked(UserId subject, String clientId, Instant issuedAt) {
            Instant since = revokedClients.get(clientId);
            return Mono.just(since != null && !issuedAt.isAfter(since));
        }
    }

    /** {@code azp} nulo omite el claim. El {@code sid} se incluye: el bearer no debe usarlo. */
    private static Jwt bearerToken(String tokenId, String authorizedParty) {
        Jwt.Builder builder = Jwt.withTokenValue("access-token")
                .header("alg", "RS256")
                .subject("keycloak-subject")
                .claim("jti", tokenId)
                .claim("sid", "keycloak-session-1")
                .issuedAt(FIXED_NOW.minusSeconds(60))
                .expiresAt(FIXED_NOW.plusSeconds(300));
        if (authorizedParty != null) {
            builder.claim("azp", authorizedParty);
        }
        return builder.build();
    }
}
