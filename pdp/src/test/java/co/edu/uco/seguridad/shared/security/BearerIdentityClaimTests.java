package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidIdentifierException;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BearerIdentityClaimTests {

    private static final UserId REAL = new UserId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final UserId FORGED = new UserId(UUID.fromString("99999999-9999-9999-9999-999999999999"));
    private static final Instant IAT = Instant.parse("2026-10-30T10:00:00Z");

    @Test
    void stamp_overwrites_an_incoming_value() {
        Jwt stamped = BearerIdentityClaim.stamp(jwtWith(FORGED.value().toString()), Optional.of(REAL));

        assertThat(stamped.getClaimAsString(BearerIdentityClaim.NAME)).isEqualTo(REAL.value().toString());
    }

    @Test
    void stamp_with_empty_removes_an_incoming_value() {
        Jwt stamped = BearerIdentityClaim.stamp(jwtWith(FORGED.value().toString()), Optional.empty());

        assertThat(stamped.getClaims()).doesNotContainKey(BearerIdentityClaim.NAME);
    }

    @Test
    void stamp_with_empty_on_a_token_without_the_claim_returns_the_same_instance() {
        Jwt jwt = jwtWith(null);

        assertThat(BearerIdentityClaim.stamp(jwt, Optional.empty())).isSameAs(jwt);
    }

    @Test
    void stamp_adds_the_claim_to_a_token_that_did_not_have_it() {
        Jwt stamped = BearerIdentityClaim.stamp(jwtWith(null), Optional.of(REAL));

        assertThat(stamped.getClaimAsString(BearerIdentityClaim.NAME)).isEqualTo(REAL.value().toString());
    }

    @Test
    void stamp_keeps_headers_and_the_rest_of_the_claims() {
        Jwt original = jwtWith(FORGED.value().toString());

        Jwt stamped = BearerIdentityClaim.stamp(original, Optional.of(REAL));

        assertThat(stamped.getTokenValue()).isEqualTo(original.getTokenValue());
        assertThat(stamped.getHeaders()).isEqualTo(original.getHeaders());
        assertThat(stamped.getSubject()).isEqualTo("sub-1");
        assertThat(stamped.getId()).isEqualTo("jti-1");
        assertThat(stamped.getIssuedAt()).isEqualTo(IAT);
        assertThat(stamped.getExpiresAt()).isEqualTo(IAT.plusSeconds(300));
        assertThat(stamped.getClaimAsString("tenant")).isEqualTo("universidad-uco");
        assertThat(stamped.getAudience()).containsExactly("pdp");
    }

    @Test
    void read_returns_empty_when_the_claim_is_absent() {
        assertThat(BearerIdentityClaim.read(jwtWith(null))).isEmpty();
    }

    @Test
    void read_returns_the_user_id_when_the_claim_is_a_uuid() {
        assertThat(BearerIdentityClaim.read(jwtWith(REAL.value().toString()))).contains(REAL);
    }

    @Test
    void read_refuses_a_value_that_is_not_a_uuid() {
        assertThatThrownBy(() -> BearerIdentityClaim.read(jwtWith("no-es-un-uuid")))
                .isInstanceOf(InvalidIdentifierException.class);
    }

    private static Jwt jwtWith(String internalClaim) {
        Jwt.Builder builder = Jwt.withTokenValue("token-value")
                .header("alg", "HS256")
                .header("kid", "k1")
                .subject("sub-1")
                .audience(List.of("pdp"))
                .claim("jti", "jti-1")
                .claim("tenant", "universidad-uco")
                .issuedAt(IAT)
                .expiresAt(IAT.plusSeconds(300));
        if (internalClaim != null) builder.claim(BearerIdentityClaim.NAME, internalClaim);
        return builder.build();
    }
}
