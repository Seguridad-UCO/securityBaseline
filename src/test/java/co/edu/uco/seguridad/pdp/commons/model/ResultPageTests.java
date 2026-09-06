package co.edu.uco.seguridad.pdp.commons.model;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidPageWindowException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Invariantes propias de {@link ResultPage}, independientes de cualquier repositorio concreto:
 * ninguna ventana nula, ningún total negativo, y el contenido nunca es la misma lista mutable que
 * entró (protege contra que un llamador la modifique después de construir la página).
 */
class ResultPageTests {

    private static final PageWindow WINDOW = PageWindow.defaultWindow();

    @Test
    void rejects_a_null_window() {
        assertThatThrownBy(() -> new ResultPage<>(List.of("a"), 1, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_null_content() {
        assertThatThrownBy(() -> new ResultPage<>(null, 0, WINDOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_a_negative_total() {
        assertThatThrownBy(() -> new ResultPage<>(List.of(), -1, WINDOW))
                .isInstanceOf(InvalidPageWindowException.class);
    }

    @Test
    void of_builds_an_equivalent_page() {
        ResultPage<String> page = ResultPage.of(List.of("a", "b"), 2, WINDOW);

        assertThat(page.content()).containsExactly("a", "b");
        assertThat(page.total()).isEqualTo(2);
        assertThat(page.window()).isEqualTo(WINDOW);
    }

    @Test
    void map_transforms_content_without_touching_total_or_window() {
        ResultPage<Integer> lengths = ResultPage.of(List.of("a", "bb", "ccc"), 3, WINDOW)
                .map(String::length);

        assertThat(lengths.content()).containsExactly(1, 2, 3);
        assertThat(lengths.total()).isEqualTo(3);
        assertThat(lengths.window()).isEqualTo(WINDOW);
    }
}
