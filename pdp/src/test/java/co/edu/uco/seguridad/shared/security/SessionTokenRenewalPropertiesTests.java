package co.edu.uco.seguridad.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-030 (D1): margen y tope de espera de la renovacion del token BFF. */
class SessionTokenRenewalPropertiesTests {

    private static final String PREFIX = "pdp.security.session";

    @Test
    void defaults_to_a_60_second_skew_and_a_5_second_timeout() {
        SessionTokenRenewalProperties properties = bind(Map.of());

        assertThat(properties.tokenRenewalSkew()).isEqualTo(Duration.ofSeconds(60));
        assertThat(properties.tokenRenewalTimeout()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void binds_both_values_from_the_session_prefix() {
        SessionTokenRenewalProperties properties = bind(Map.of(
                PREFIX + ".token-renewal-skew", "30s",
                PREFIX + ".token-renewal-timeout", "2s"));

        assertThat(properties.tokenRenewalSkew()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.tokenRenewalTimeout()).isEqualTo(Duration.ofSeconds(2));
    }

    @Test
    void binding_one_value_keeps_the_default_of_the_other() {
        SessionTokenRenewalProperties properties = bind(Map.of(PREFIX + ".token-renewal-skew", "10s"));

        assertThat(properties.tokenRenewalSkew()).isEqualTo(Duration.ofSeconds(10));
        assertThat(properties.tokenRenewalTimeout()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void accepts_a_zero_skew() {
        SessionTokenRenewalProperties properties = new SessionTokenRenewalProperties(Duration.ZERO, Duration.ofSeconds(5));

        assertThat(properties.tokenRenewalSkew()).isEqualTo(Duration.ZERO);
    }

    @Test
    void rejects_a_negative_skew() {
        assertThatThrownBy(() -> new SessionTokenRenewalProperties(Duration.ofSeconds(-1), Duration.ofSeconds(5)))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    private static SessionTokenRenewalProperties bind(Map<String, String> source) {
        return new Binder(new MapConfigurationPropertySource(source))
                .bindOrCreate(PREFIX, SessionTokenRenewalProperties.class);
    }
}
