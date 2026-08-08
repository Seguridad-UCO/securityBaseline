package co.edu.uco.seguridad.pdp.commons;

import co.edu.uco.seguridad.crosscutting.messages.ValueObjectMessages;
import co.edu.uco.seguridad.pdp.commons.exception.InvalidPageWindowException;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Porción inmutable de una consulta de catálogo, llevando la ventana que la produjo para que los
 * llamadores nunca tengan que adivinar de dónde proviene el contenido.
 */
public record ResultPage<T>(List<T> content, long total, PageWindow window) {

    public ResultPage {
        Objects.requireNonNull(window, ValueObjectMessages.ResultPage.WINDOW_REQUIRED);
        content = List.copyOf(Objects.requireNonNull(content, ValueObjectMessages.ResultPage.CONTENT_REQUIRED));
        if (total < 0) {
            throw new InvalidPageWindowException(ValueObjectMessages.ResultPage.NEGATIVE_TOTAL);
        }
    }

    public static <T> ResultPage<T> of(List<T> content, long total, PageWindow window) {
        return new ResultPage<>(content, total, window);
    }

    public <R> ResultPage<R> map(Function<? super T, ? extends R> mapper) {
        return new ResultPage<>(content.stream().<R>map(mapper).toList(), total, window);
    }
}
