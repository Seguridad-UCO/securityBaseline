package co.edu.uco.seguridad.pdp.resources.domain;

import co.edu.uco.seguridad.pdp.resources.domain.exception.InvalidResourcePathException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourcePathTests {

    @Test
    void accepts_a_simple_path() {
        assertThat(new ResourcePath("/estudiantes").value()).isEqualTo("/estudiantes");
    }

    @Test
    void accepts_a_path_with_a_template_segment() {
        assertThat(new ResourcePath("/estudiantes/{id}").value()).isEqualTo("/estudiantes/{id}");
    }

    @Test
    void rejects_a_null_value() {
        assertThatThrownBy(() -> new ResourcePath(null)).isInstanceOf(InvalidResourcePathException.class);
    }

    @Test
    void rejects_a_path_not_starting_with_a_slash() {
        assertThatThrownBy(() -> new ResourcePath("estudiantes")).isInstanceOf(InvalidResourcePathException.class);
    }

    @Test
    void rejects_a_path_with_a_double_slash() {
        assertThatThrownBy(() -> new ResourcePath("/estudiantes//activos"))
                .isInstanceOf(InvalidResourcePathException.class);
    }
}
