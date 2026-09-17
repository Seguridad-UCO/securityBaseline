package co.edu.uco.seguridad.shared.security.revocation;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.shared.security.TestJwtSupport;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

/**
 * PLAN-HU-022.md §9 — cinco casos, unitaria, sin contexto de Spring: los tres colaboradores
 * ({@link ReactiveJwtDecoder} delegado, {@link TokenRevocationPort}, {@link SubjectUserIdLookupValidator})
 * son interfaces funcionales, así que se doblan con lambdas (sb-testing).
 *
 * <p>Caso (a) fija el contrato resuelto en PLAN-HU-022.md §11 punto 4: ausencia de {@code UserId}
 * para un subject válido del JWT es fail-closed, y {@code isRevoked} nunca se llega a consultar.</p>
 */
class RevocationAwareJwtDecoderTests {

    private static final String SUBJECT = "test-subject";
    private static final Jwt VALID_JWT = TestJwtSupport.jwt("universidad-uco", SUBJECT);
    private static final UserId SUBJECT_USER_ID = new UserId(UUID.randomUUID());

    @Test
    void rejects_when_the_subject_user_id_lookup_cannot_resolve_a_user_id() {
        RevocationAwareJwtDecoder decoder = new RevocationAwareJwtDecoder(
                delegateReturning(VALID_JWT), unreachableRevocation(), subjectLookupReturning(Mono.empty()));

        StepVerifier.create(decoder.decode("token"))
                .expectError(JwtException.class)
                .verify();
    }

    @Test
    void passes_the_jwt_through_when_it_is_not_revoked() {
        RevocationAwareJwtDecoder decoder = new RevocationAwareJwtDecoder(
                delegateReturning(VALID_JWT), revocationAnswering(false), subjectLookupReturning(Mono.just(SUBJECT_USER_ID)));

        StepVerifier.create(decoder.decode("token"))
                .expectNext(VALID_JWT)
                .verifyComplete();
    }

    @Test
    void rejects_with_a_jwt_validation_exception_when_it_is_revoked() {
        RevocationAwareJwtDecoder decoder = new RevocationAwareJwtDecoder(
                delegateReturning(VALID_JWT), revocationAnswering(true), subjectLookupReturning(Mono.just(SUBJECT_USER_ID)));

        StepVerifier.create(decoder.decode("token"))
                .expectError(JwtValidationException.class)
                .verify();
    }

    @Test
    void rejects_when_the_revocation_check_itself_fails() {
        RuntimeException redisDown = new RuntimeException("redis no disponible");
        RevocationAwareJwtDecoder decoder = new RevocationAwareJwtDecoder(
                delegateReturning(VALID_JWT), revocationFailing(redisDown), subjectLookupReturning(Mono.just(SUBJECT_USER_ID)));

        StepVerifier.create(decoder.decode("token"))
                .expectError(JwtException.class)
                .verify();
    }

    @Test
    void never_checks_revocation_when_the_delegate_itself_rejects_the_token() {
        JwtException signatureRejected = new JwtException("firma inválida");
        RevocationAwareJwtDecoder decoder = new RevocationAwareJwtDecoder(
                delegateFailing(signatureRejected), unreachableRevocation(), unreachableSubjectLookup());

        StepVerifier.create(decoder.decode("token"))
                .expectErrorMessage("firma inválida")
                .verify();
    }

    private static ReactiveJwtDecoder delegateReturning(Jwt jwt) {
        return token -> Mono.just(jwt);
    }

    private static ReactiveJwtDecoder delegateFailing(JwtException failure) {
        return token -> Mono.error(failure);
    }

    private static SubjectUserIdLookupValidator subjectLookupReturning(Mono<UserId> result) {
        return subject -> result;
    }

    private static SubjectUserIdLookupValidator unreachableSubjectLookup() {
        return subject -> {
            throw new AssertionError("must not look up the subject when the delegate already rejected the token");
        };
    }

    private static TokenRevocationPort revocationAnswering(boolean revoked) {
        return new TokenRevocationPort() {
            @Override
            public Mono<Void> revokeAllSince(UserId subject, Instant since) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
                return Mono.just(revoked);
            }
        };
    }

    private static TokenRevocationPort revocationFailing(RuntimeException failure) {
        return new TokenRevocationPort() {
            @Override
            public Mono<Void> revokeAllSince(UserId subject, Instant since) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
                return Mono.error(failure);
            }
        };
    }

    private static TokenRevocationPort unreachableRevocation() {
        return new TokenRevocationPort() {
            @Override
            public Mono<Void> revokeAllSince(UserId subject, Instant since) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
                throw new AssertionError("must not check revocation when there is no user id to check it for");
            }
        };
    }
}
