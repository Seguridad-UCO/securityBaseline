package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * Nivel uno para la consulta de catálogo: la cadena de consulta exactamente como llegó, todos los
 * campos nulables porque "ausente" es una respuesta distinta de "presente pero incorrecto". Sin
 * {@code tenantId}: sale del token autenticado (ADR-018).
 */
public record SearchProtectedApplicationsRawRequest(String nameContains,
                                                    String resourceContains,
                                                    String page,
                                                    String size,
                                                    String offset,
                                                    String limit) {
}
