package co.edu.uco.seguridad.pdp.resources.domain.model;

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
    void matches_a_concrete_path_against_a_template_segment() {
        assertThat(new ResourcePath("/api/v1/empleado/{id}")
                .matches(new ResourcePath("/api/v1/empleado/42"))).isTrue();
    }

    @Test
    void matches_multiple_template_segments_in_a_path() {
        assertThat(new ResourcePath("/api/v1/empleados/{id}/tipo/{tipo}")
                .matches(new ResourcePath("/api/v1/empleados/42/tipo/contratista"))).isTrue();
    }

    @Test
    void does_not_match_different_path_shapes() {
        assertThat(new ResourcePath("/api/v1/empleado/{id}")
                .matches(new ResourcePath("/api/v1/empleado/42/tipo/planta"))).isFalse();
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
