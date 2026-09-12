package co.edu.uco.seguridad.shared.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsPropertiesTests {

    @Test
    void a_null_origin_list_becomes_empty() {
        var properties = new CorsProperties(null);

        assertThat(properties.allowedOrigins()).isEmpty();
    }

    @Test
    void keeps_the_configured_origins() {
        var properties = new CorsProperties(List.of("http://localhost:5173"));

        assertThat(properties.allowedOrigins()).containsExactly("http://localhost:5173");
    }
}
