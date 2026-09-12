package co.edu.uco.seguridad.pdp.resources.domain.model;

import co.edu.uco.seguridad.pdp.resources.domain.exception.UnsupportedHttpMethodException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpVerbTests {

    @Test
    void parses_case_insensitively() {
        assertThat(HttpVerb.parse("get")).isEqualTo(HttpVerb.GET);
        assertThat(HttpVerb.parse("Post")).isEqualTo(HttpVerb.POST);
    }

    @Test
    void rejects_a_blank_value() {
        assertThatThrownBy(() -> HttpVerb.parse(" ")).isInstanceOf(UnsupportedHttpMethodException.class);
    }

    @Test
    void rejects_a_verb_the_platform_does_not_support() {
        assertThatThrownBy(() -> HttpVerb.parse("TRACE")).isInstanceOf(UnsupportedHttpMethodException.class);
    }
}
