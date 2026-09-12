package co.edu.uco.seguridad.shared.config;

import co.edu.uco.seguridad.shared.security.JwtSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigurationTests {

    private static final String ISSUER = "https://issuer.test";
    private static final String AUDIENCE = "test-audience";

    private final SecurityConfiguration configuration = new SecurityConfiguration();

    @Test
    void builds_an_hmac_decoder_when_a_secret_is_configured() {
        var properties = new JwtSecurityProperties("a-secret-value", null, ISSUER, AUDIENCE);

        ReactiveJwtDecoder decoder = configuration.jwtDecoder(properties);

        assertThat(decoder).isNotNull();
    }

    @Test
    void builds_a_jwks_decoder_when_a_jwk_set_uri_is_configured() {
        var properties = new JwtSecurityProperties(null, "https://idp.test/certs", ISSUER, AUDIENCE);

        ReactiveJwtDecoder decoder = configuration.jwtDecoder(properties);

        assertThat(decoder).isNotNull();
    }
}
