package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * Nivel uno de la estrategia de entrada en dos niveles (HU-010): el JSON exactamente como llegó,
 * todo {@code String} y sin validar a propósito. Sin {@code tenantId}: sale del token autenticado
 * (ADR-018).
 */
public record RegisterApplicationWithInitialResourceRawRequest(String name, String description, String baseUrl,
        String resourcePath, String resourceMethod) {
}
