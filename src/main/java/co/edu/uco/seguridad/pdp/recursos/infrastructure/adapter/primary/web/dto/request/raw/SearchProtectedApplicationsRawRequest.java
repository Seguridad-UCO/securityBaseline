package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * Nivel uno para la consulta de catálogo: la cadena de consulta exactamente como llegó.
 *
 * <p>Todos los campos son {@code String} y todos son nulables, porque para una búsqueda "ausente" es una
 * respuesta significativa y debe distinguirse de "presente pero incorrecto".</p>
 *
 * <p>No lleva {@code tenantId}: desde ADR-0003 toda consulta está acotada al tenant del token
 * autenticado, no a uno que el llamador elija por parámetro.</p>
 */
public record SearchProtectedApplicationsRawRequest(String nameContains,
                                                    String resourceContains,
                                                    String page,
                                                    String size,
                                                    String offset,
                                                    String limit) {
}
