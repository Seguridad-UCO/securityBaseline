package co.edu.uco.seguridad.shared.web;

import java.util.List;

/**
 * Forma HTTP de un resultado paginado: las filas más los metadatos que un cliente necesita para pedir la siguiente
 * porción, expresados como número de página e intervalo explícito para que cualquier estilo de cliente sea
 * atendido por una sola carga. {@code limit} sirve tanto de tamaño de página como de longitud del intervalo — un
 * cliente que piensa en página/tamaño lee {@code page}+{@code limit}; uno que piensa en offset/limit lee
 * {@code offset}+{@code limit}. No hay un campo {@code size} aparte: siempre iba a valer lo mismo que
 * {@code limit}, así que era un nombre distinto para el mismo número, no información adicional.
 */
public record PageResponse<T>(List<T> content, long total, int page, int offset, int limit) {

    public PageResponse {
        content = List.copyOf(content);
    }
}
