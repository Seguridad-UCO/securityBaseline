package co.edu.uco.seguridad.pdp.commons.model;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidPageWindowException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Criterios 18 y 19: paginación e intervalos son la misma ventana acotada, y el límite no puede ser
 * escapado porque se aplica por el constructor en lugar de por una verificación del lado del llamador.
 */
class PageWindowTests {

    @Test
    void translates_a_page_into_an_offset() {
        PageWindow window = PageWindow.ofPage(2, 25);

        assertThat(window.offset()).isEqualTo(50);
        assertThat(window.limit()).isEqualTo(25);
        assertThat(window.page()).isEqualTo(2);
    }

    @Test
    void expresses_a_range_directly() {
        PageWindow window = PageWindow.ofRange(10, 5);

        assertThat(window.offset()).isEqualTo(10);
        assertThat(window.limit()).isEqualTo(5);
    }

    @ParameterizedTest
    @CsvSource({"-1, 10", "0, 0", "0, 101"})
    void rejects_pages_outside_the_protective_limits(int page, int size) {
        assertThatThrownBy(() -> PageWindow.ofPage(page, size))
                .isInstanceOf(InvalidPageWindowException.class);
    }

    @ParameterizedTest
    @CsvSource({"-1, 10", "0, 0", "0, 101"})
    void rejects_ranges_outside_the_protective_limits(int offset, int limit) {
        assertThatThrownBy(() -> PageWindow.ofRange(offset, limit))
                .isInstanceOf(InvalidPageWindowException.class);
    }

    @Test
    void accepts_the_maximum_limit_but_nothing_beyond_it() {
        assertThat(PageWindow.ofRange(0, PageWindow.MAX_LIMIT).limit()).isEqualTo(100);
        assertThatThrownBy(() -> PageWindow.ofRange(0, PageWindow.MAX_LIMIT + 1))
                .isInstanceOf(InvalidPageWindowException.class);
    }

    @Test
    void result_page_copies_its_content_so_callers_cannot_mutate_it() {
        List<String> mutable = new ArrayList<>(List.of("a"));
        ResultPage<String> page = ResultPage.of(mutable, 1, PageWindow.defaultWindow());

        mutable.add("b");

        assertThat(page.content()).containsExactly("a");
    }

    @Test
    void result_page_maps_content_while_preserving_total_and_window() {
        ResultPage<Integer> page = ResultPage.of(List.of(1, 2), 7, PageWindow.ofRange(3, 2));

        ResultPage<String> mapped = page.map(String::valueOf);

        assertThat(mapped.content()).containsExactly("1", "2");
        assertThat(mapped.total()).isEqualTo(7);
        assertThat(mapped.window()).isEqualTo(PageWindow.ofRange(3, 2));
    }
}
