package co.edu.uco.seguridad.pdp.commons;

import co.edu.uco.seguridad.crosscutting.messages.ValueObjectMessages;
import co.edu.uco.seguridad.pdp.commons.exception.InvalidPageWindowException;

/**
 * Porción acotada de un conjunto de resultados, expresada como {@code offset + limit}.
 *
 * <p>Página/tamaño (criterio 18) e intervalos explícitos (criterio 19) son dos formas de nombrar la misma
 * ventana, por lo que ambas fábricas convergen aquí y cada búsqueda está acotada por construcción: no hay
 * forma de pedir al catálogo un número ilimitado de filas.</p>
 */
public record PageWindow(int offset, int limit) {

    public static final int MAX_LIMIT = 100;
    public static final int DEFAULT_LIMIT = 20;

    public PageWindow {
        if (offset < 0) {
            throw new InvalidPageWindowException(ValueObjectMessages.PageWindow.NEGATIVE_OFFSET);
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new InvalidPageWindowException(ValueObjectMessages.PageWindow.LIMIT_RANGE);
        }
    }

    /** Páginas numeradas, basadas en cero, expuestas por {@code ?page=&size=}. */
    public static PageWindow ofPage(int page, int size) {
        if (page < 0) {
            throw new InvalidPageWindowException(ValueObjectMessages.PageWindow.NEGATIVE_PAGE);
        }
        if (size < 1 || size > MAX_LIMIT) {
            throw new InvalidPageWindowException(ValueObjectMessages.PageWindow.SIZE_RANGE);
        }
        return new PageWindow(Math.multiplyExact(page, size), size);
    }

    /** Intervalo explícito, expuesto por {@code ?offset=&limit=}. */
    public static PageWindow ofRange(int offset, int limit) {
        return new PageWindow(offset, limit);
    }

    public static PageWindow defaultWindow() {
        return new PageWindow(0, DEFAULT_LIMIT);
    }

    public int page() {
        return offset / limit;
    }
}
