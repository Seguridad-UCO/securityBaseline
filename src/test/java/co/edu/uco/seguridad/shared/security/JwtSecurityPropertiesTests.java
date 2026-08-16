package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.crosscutting.messages.RequiredArgumentMessages;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSecurityPropertiesTests {

    private static final String ISSUER = "https://issuer.test";
    private static final String AUDIENCE = "test-audience";

    @Test
    void hmac_mode_is_accepted_and_does_not_use_jwks() {
        var properties = new JwtSecurityProperties("a-secret", null, ISSUER, AUDIENCE);

        assertThat(properties.usesJwks()).isFalse();
    }

    @Test
    void jwks_mode_is_accepted_and_uses_jwks() {
        var properties = new JwtSecurityProperties(null, "https://idp.test/certs", ISSUER, AUDIENCE);

        assertThat(properties.usesJwks()).isTrue();
    }

    @Test
    void rejects_neither_secret_nor_jwk_set_uri() {
        assertThatThrownBy(() -> new JwtSecurityProperties(null, null, ISSUER, AUDIENCE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(RequiredArgumentMessages.JWT_EXACTLY_ONE_MODE);
    }

    @Test
    void rejects_both_secret_and_jwk_set_uri() {
        assertThatThrownBy(() -> new JwtSecurityProperties("a-secret", "https://idp.test/certs", ISSUER, AUDIENCE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(RequiredArgumentMessages.JWT_EXACTLY_ONE_MODE);
    }

    @Test
    void rejects_a_blank_secret_as_absent() {
        assertThatThrownBy(() -> new JwtSecurityProperties(" ", null, ISSUER, AUDIENCE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(RequiredArgumentMessages.JWT_EXACTLY_ONE_MODE);
    }

    @Test
    void rejects_a_missing_issuer() {
        assertThatThrownBy(() -> new JwtSecurityProperties("a-secret", null, null, AUDIENCE))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(RequiredArgumentMessages.JWT_ISSUER);
    }

    @Test
    void rejects_a_missing_audience() {
        assertThatThrownBy(() -> new JwtSecurityProperties("a-secret", null, ISSUER, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(RequiredArgumentMessages.JWT_AUDIENCE);
    }
}
