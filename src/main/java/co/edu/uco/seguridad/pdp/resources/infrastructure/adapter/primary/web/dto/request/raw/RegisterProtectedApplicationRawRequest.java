package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * Nivel uno de la estrategia de entrada en dos niveles: el JSON exactamente como llegó, to-do
 * {@code String} y sin validar a propósito, así el valor incorrecto llega a nuestro código en vez de
 * un 400 genérico de framework. Sin {@code tenantId}: sale del token autenticado (ADR-018).
 */
public record RegisterProtectedApplicationRawRequest(String applicationName,
                                                     String resourceCode,
                                                     String action) {
}
