package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidIdentifierException;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-038: {@code PdpPrincipal.from(Jwt)} lee el claim interno que sella el decoder Bearer. */
class PdpPrincipalBearerIdentityTests {

    private static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void from_jwt_exposes_the_user_id_stamped_by_the_decoder() {
        PdpPrincipal principal = PdpPrincipal.from(jwtWith(USER.toString()));

        assertThat(principal.userId()).contains(new UserId(USER));
        assertThat(principal.tenantId().value()).isEqualTo("universidad-uco");
        assertThat(principal.subject()).isEqualTo("sub-1");
    }

    @Test
    void from_jwt_without_the_claim_has_no_user_id() {
        assertThat(PdpPrincipal.from(jwtWith(null)).userId()).isEmpty();
    }

    @Test
    void from_jwt_with_a_garbage_claim_fails() {
        assertThatThrownBy(() -> PdpPrincipal.from(jwtWith("basura")))
                .isInstanceOf(InvalidIdentifierException.class);
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
