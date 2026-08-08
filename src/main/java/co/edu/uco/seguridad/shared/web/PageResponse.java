package co.edu.uco.seguridad.shared.web;

import java.util.List;

/**
 * Forma HTTP de un resultado paginado: las filas más los metadatos que un cliente necesita para pedir la siguiente
 * porción, expresados como número de página e intervalo explícito para que cualquier estilo de cliente sea
 * atendido por una sola carga.
 */
public record PageResponse<T>(List<T> content, long total, int page, int size, int offset, int limit) {

    public PageResponse {
        content = List.copyOf(content);
    }
}
