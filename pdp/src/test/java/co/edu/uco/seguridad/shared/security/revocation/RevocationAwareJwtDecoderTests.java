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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

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
    private static final String TECHNICAL_CLIENT = "security-pep-internal";
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
    void rejects_when_the_exact_jti_is_revoked_without_revoking_other_subject_tokens() {
        TokenRevocationPort exactTokenRevocation = new TokenRevocationPort() {
            @Override
            public Mono<Void> revokeAllSince(UserId subject, Instant since) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Void> revokeToken(UserId subject, String tokenId, Instant expiresAt) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
                return Mono.just(false);
            }

            @Override
            public Mono<Boolean> isTokenRevoked(UserId subject, String tokenId) {
                return Mono.just(VALID_JWT.getId().equals(tokenId));
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
        RevocationAwareJwtDecoder decoder = new RevocationAwareJwtDecoder(
                delegateReturning(VALID_JWT), exactTokenRevocation, subjectLookupReturning(Mono.just(SUBJECT_USER_ID)));

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

    @Test
    void passes_a_valid_technical_pep_token_without_requiring_a_human_identity() {
        Jwt technicalToken = Jwt.withTokenValue("technical-token")
                .header("alg", "RS256")
                .subject("service-account-security-pep-internal")
                .issuer(TestJwtSupport.ISSUER)
                .audience(java.util.List.of(TestJwtSupport.AUDIENCE))
                .claim("azp", TECHNICAL_CLIENT)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        RevocationAwareJwtDecoder decoder = new RevocationAwareJwtDecoder(
                delegateReturning(technicalToken), unreachableRevocation(), unreachableSubjectLookup(), TECHNICAL_CLIENT);

        StepVerifier.create(decoder.decode("token"))
                .expectNext(technicalToken)
                .verifyComplete();
    }

    @Test
    void rejects_when_the_client_of_the_token_is_revoked_for_the_subject() {
        List<ClientQuery> queries = new ArrayList<>();
        RevocationAwareJwtDecoder decoder = new RevocationAwareJwtDecoder(
                delegateReturning(jwtIssuedTo("app-a")), revocationWithClientState(queries, query -> Mono.just(true)),
                subjectLookupReturning(Mono.just(SUBJECT_USER_ID)));

        StepVerifier.create(decoder.decode("token"))
                .expectError(JwtValidationException.class)
                .verify();
        assertThat(queries).extracting(ClientQuery::clientId).containsExactly("app-a");
    }

    @Test
    void passes_the_jwt_through_when_its_client_is_not_revoked() {
        Jwt jwt = jwtIssuedTo("app-a");
        List<ClientQuery> queries = new ArrayList<>();
        RevocationAwareJwtDecoder decoder = new RevocationAwareJwtDecoder(
                delegateReturning(jwt), revocationWithClientState(queries, query -> Mono.just(false)),
                subjectLookupReturning(Mono.just(SUBJECT_USER_ID)));

        StepVerifier.create(decoder.decode("token"))
                .expectNext(jwt)
                .verifyComplete();
        assertThat(queries).hasSize(1);
        assertThat(queries.getFirst().subject()).isEqualTo(SUBJECT_USER_ID);
        assertThat(queries.getFirst().clientId()).isEqualTo("app-a");
        assertThat(queries.getFirst().issuedAt()).isEqualTo(jwt.getIssuedAt());
    }

    @Test
    void does_not_reject_a_token_of_another_client_of_the_same_subject() {
        Jwt jwt = jwtIssuedTo("app-b");
        List<ClientQuery> queries = new ArrayList<>();
        RevocationAwareJwtDecoder decoder = new RevocationAwareJwtDecoder(
                delegateReturning(jwt),
                revocationWithClientState(queries, query -> Mono.just("app-a".equals(query.clientId()))),
                subjectLookupReturning(Mono.just(SUBJECT_USER_ID)));

        StepVerifier.create(decoder.decode("token"))
                .expectNext(jwt)
                .verifyComplete();
        assertThat(queries).extracting(ClientQuery::clientId).containsExactly("app-b");
    }

    @Test
    void does_not_reject_a_token_of_another_subject_when_only_this_subject_client_is_revoked() {
        UserId otherUser = new UserId(UUID.randomUUID());
        Jwt jwt = jwtIssuedTo("app-a");
        List<ClientQuery> queries = new ArrayList<>();
        RevocationAwareJwtDecoder decoder = new RevocationAwareJwtDecoder(
                delegateReturning(jwt),
                revocationWithClientState(queries, query -> Mono.just(SUBJECT_USER_ID.equals(query.subject()))),
                subjectLookupReturning(Mono.just(otherUser)));

        StepVerifier.create(decoder.decode("token"))
                .expectNext(jwt)
                .verifyComplete();
        assertThat(queries).extracting(ClientQuery::subject).containsExactly(otherUser);
    }

    @Test
    void does_not_query_client_revocation_when_the_token_has_no_azp() {
        RevocationAwareJwtDecoder decoder = new RevocationAwareJwtDecoder(
                delegateReturning(VALID_JWT), revocationForbiddingClientQueries(),
                subjectLookupReturning(Mono.just(SUBJECT_USER_ID)));

        StepVerifier.create(decoder.decode("token"))
                .expectNext(VALID_JWT)
                .verifyComplete();
    }

    @Test
    void treats_a_blank_azp_as_absent_and_does_not_query_client_revocation() {
        Jwt jwt = jwtIssuedTo("   ");
        RevocationAwareJwtDecoder decoder = new RevocationAwareJwtDecoder(
                delegateReturning(jwt), revocationForbiddingClientQueries(),
                subjectLookupReturning(Mono.just(SUBJECT_USER_ID)));

        StepVerifier.create(decoder.decode("token"))
                .expectNext(jwt)
                .verifyComplete();
    }

    @Test
    void rejects_when_the_client_revocation_check_itself_fails() {
        RuntimeException redisDown = new RuntimeException("redis no disponible");
        RevocationAwareJwtDecoder decoder = new RevocationAwareJwtDecoder(
                delegateReturning(jwtIssuedTo("app-a")),
                revocationWithClientState(new ArrayList<>(), query -> Mono.error(redisDown)),
                subjectLookupReturning(Mono.just(SUBJECT_USER_ID)));

        StepVerifier.create(decoder.decode("token"))
                .expectError(JwtValidationException.class)
                .verify();
    }

    private record ClientQuery(UserId subject, String clientId, Instant issuedAt) {
    }

    private static Jwt jwtIssuedTo(String clientId) {
        return Jwt.withTokenValue("client-token")
                .header("alg", "RS256")
                .subject(SUBJECT)
                .issuer(TestJwtSupport.ISSUER)
                .audience(List.of(TestJwtSupport.AUDIENCE))
                .claim("jti", "jti-client-token")
                .claim("azp", clientId)
                .issuedAt(Instant.parse("2026-10-28T10:00:00Z"))
                .expiresAt(Instant.parse("2026-10-28T11:00:00Z"))
                .build();
    }

    /** {@code isRevoked} e {@code isTokenRevoked} responden false; solo la consulta por cliente es configurable. */
    private static TokenRevocationPort revocationWithClientState(List<ClientQuery> queries,
                                                                 Function<ClientQuery, Mono<Boolean>> clientAnswer) {
        return new TokenRevocationPort() {
            @Override
            public Mono<Void> revokeAllSince(UserId subject, Instant since) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Void> revokeToken(UserId subject, String tokenId, Instant expiresAt) {
                throw new UnsupportedOperationException();
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
                ClientQuery query = new ClientQuery(subject, clientId, issuedAt);
                queries.add(query);
                return clientAnswer.apply(query);
            }
        };
    }

    private static TokenRevocationPort revocationForbiddingClientQueries() {
        return new TokenRevocationPort() {
            @Override
            public Mono<Void> revokeAllSince(UserId subject, Instant since) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Void> revokeToken(UserId subject, String tokenId, Instant expiresAt) {
                throw new UnsupportedOperationException();
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
                throw new AssertionError("must not query client revocation for a token without azp");
            }
        };
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
            public Mono<Void> revokeToken(UserId subject, String tokenId, Instant expiresAt) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
                return Mono.just(revoked);
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
    }

    private static TokenRevocationPort revocationFailing(RuntimeException failure) {
        return new TokenRevocationPort() {
            @Override
            public Mono<Void> revokeAllSince(UserId subject, Instant since) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Void> revokeToken(UserId subject, String tokenId, Instant expiresAt) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
                return Mono.error(failure);
            }

            @Override
            public Mono<Boolean> isTokenRevoked(UserId subject, String tokenId) {
                return Mono.error(failure);
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
    }

    private static TokenRevocationPort unreachableRevocation() {
        return new TokenRevocationPort() {
            @Override
            public Mono<Void> revokeAllSince(UserId subject, Instant since) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Void> revokeToken(UserId subject, String tokenId, Instant expiresAt) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
                throw new AssertionError("must not check revocation when there is no user id to check it for");
            }

            @Override
            public Mono<Boolean> isTokenRevoked(UserId subject, String tokenId) {
                throw new AssertionError("must not check revocation when there is no user id to check it for");
            }

            @Override
            public Mono<Void> revokeClientSince(UserId subject, String clientId, Instant since) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Boolean> isClientRevoked(UserId subject, String clientId, Instant issuedAt) {
                throw new AssertionError("must not check revocation when there is no user id to check it for");
            }
        };
    }
}
