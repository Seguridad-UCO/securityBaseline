package co.edu.uco.seguridad.pdp.applications.domain;

import co.edu.uco.seguridad.pdp.applications.domain.exception.InvalidApplicationBaseUrlException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationBaseUrlTests {

    @Test
    void accepts_an_absolute_url_with_scheme_and_host() {
        assertThat(new ApplicationBaseUrl("https://gestion-academica.uco.edu.co").value())
                .isEqualTo("https://gestion-academica.uco.edu.co");
    }

    @Test
    void rejects_a_null_value() {
        assertThatThrownBy(() -> new ApplicationBaseUrl(null))
                .isInstanceOf(InvalidApplicationBaseUrlException.class);
    }

    @Test
    void rejects_a_relative_path_without_a_host() {
        assertThatThrownBy(() -> new ApplicationBaseUrl("/no-host"))
                .isInstanceOf(InvalidApplicationBaseUrlException.class);
    }

    @Test
    void rejects_malformed_uri_syntax() {
        assertThatThrownBy(() -> new ApplicationBaseUrl("https://exa mple.com"))
                .isInstanceOf(InvalidApplicationBaseUrlException.class);
    }
}
