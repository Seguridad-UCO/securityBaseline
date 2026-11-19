package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
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

import static org.assertj.core.api.Assertions.assertThat;

class LocalIdentityStampingJwtDecoderTests {

    private static final UserId REAL = new UserId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final UserId FORGED = new UserId(UUID.fromString("99999999-9999-9999-9999-999999999999"));

    @Test
    void stamps_the_user_id_of_a_subject_that_has_an_identity() {
        var decoder = new LocalIdentityStampingJwtDecoder(delegateReturning(jwtWith(null)), subject -> Mono.just(REAL));

        StepVerifier.create(decoder.decode("t"))
                .assertNext(jwt -> assertThat(jwt.getClaimAsString(BearerIdentityClaim.NAME))
                        .isEqualTo(REAL.value().toString()))
                .verifyComplete();
    }

    @Test
    void lets_a_subject_without_identity_through_with_no_claim() {
        var decoder = new LocalIdentityStampingJwtDecoder(delegateReturning(jwtWith(null)), subject -> Mono.empty());

        StepVerifier.create(decoder.decode("t"))
                .assertNext(jwt -> assertThat(jwt.getClaims()).doesNotContainKey(BearerIdentityClaim.NAME))
                .verifyComplete();
    }

    @Test
    void removes_a_forged_claim_when_the_subject_has_no_identity() {
        var decoder = new LocalIdentityStampingJwtDecoder(
                delegateReturning(jwtWith(FORGED.value().toString())), subject -> Mono.empty());

        StepVerifier.create(decoder.decode("t"))
                .assertNext(jwt -> assertThat(jwt.getClaims()).doesNotContainKey(BearerIdentityClaim.NAME))
                .verifyComplete();
    }

    @Test
    void overwrites_a_forged_claim_with_the_real_user_id() {
        var decoder = new LocalIdentityStampingJwtDecoder(
                delegateReturning(jwtWith(FORGED.value().toString())), subject -> Mono.just(REAL));

        StepVerifier.create(decoder.decode("t"))
                .assertNext(jwt -> assertThat(jwt.getClaimAsString(BearerIdentityClaim.NAME))
                        .isEqualTo(REAL.value().toString()))
                .verifyComplete();
    }

    @Test
    void looks_the_user_up_by_the_subject_of_the_token() {
        List<String> looked = new ArrayList<>();
        SubjectUserIdLookupValidator lookup = subject -> {
            looked.add(subject);
            return Mono.just(REAL);
        };

        StepVerifier.create(new LocalIdentityStampingJwtDecoder(delegateReturning(jwtWith(null)), lookup).decode("t"))
                .expectNextCount(1)
                .verifyComplete();

        assertThat(looked).containsExactly("sub-1");
    }

    @Test
    void a_lookup_failure_becomes_a_jwt_validation_error() {
        var decoder = new LocalIdentityStampingJwtDecoder(delegateReturning(jwtWith(null)),
                subject -> Mono.error(new IllegalStateException("surreal caido")));

        StepVerifier.create(decoder.decode("t"))
                .expectError(JwtValidationException.class)
                .verify();
    }

    @Test
    void a_delegate_failure_propagates_without_consulting_the_lookup() {
        List<String> looked = new ArrayList<>();
        ReactiveJwtDecoder failing = token -> Mono.error(new JwtException("firma invalida"));
        var decoder = new LocalIdentityStampingJwtDecoder(failing, subject -> {
            looked.add(subject);
            return Mono.just(REAL);
        });

        StepVerifier.create(decoder.decode("t"))
                .expectErrorSatisfies(error -> assertThat(error).isInstanceOf(JwtException.class)
                        .hasMessage("firma invalida"))
                .verify();

        assertThat(looked).isEmpty();
    }

    private static ReactiveJwtDecoder delegateReturning(Jwt jwt) {
        return token -> Mono.just(jwt);
    }

    private static Jwt jwtWith(String internalClaim) {
        Instant iat = Instant.parse("2026-10-30T10:00:00Z");
        Jwt.Builder builder = Jwt.withTokenValue("token-value")
                .header("alg", "HS256")
                .subject("sub-1")
                .claim("jti", "jti-1")
                .claim("tenant", "universidad-uco")
                .issuedAt(iat)
                .expiresAt(iat.plusSeconds(300));
        if (internalClaim != null) builder.claim(BearerIdentityClaim.NAME, internalClaim);
        return builder.build();
    }
}
