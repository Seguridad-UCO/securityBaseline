package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * Nivel uno de la estrategia de entrada en dos niveles: el JSON exactamente como llegó, todo
 * {@code String} y sin validar a propósito. Sin {@code tenantId}: sale del token autenticado
 * (ADR-018).
 */
public record RegisterApplicationRawRequest(String name, String description, String baseUrl) {
}
