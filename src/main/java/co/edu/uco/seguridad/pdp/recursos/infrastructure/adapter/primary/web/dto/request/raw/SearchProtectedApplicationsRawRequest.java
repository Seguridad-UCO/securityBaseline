package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * Nivel uno para la consulta de catálogo: la cadena de consulta exactamente como llegó.
 *
 * <p>Todos los campos son {@code String} y todos son nulables, porque para una búsqueda "ausente" es una
 * respuesta significativa y debe distinguirse de "presente pero incorrecto".</p>
 */
public record SearchProtectedApplicationsRawRequest(String tenantId,
                                                    String nameContains,
                                                    String resourceContains,
                                                    String page,
                                                    String size,
                                                    String offset,
                                                    String limit) {
}
