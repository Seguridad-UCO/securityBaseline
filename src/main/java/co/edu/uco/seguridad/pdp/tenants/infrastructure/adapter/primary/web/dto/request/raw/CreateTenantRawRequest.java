package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * Nivel uno de la estrategia de entrada en dos niveles: el JSON exactamente como llegó, todo
 * {@code String} y sin validar a propósito, así el valor incorrecto llega a nuestro código en vez de
 * un 400 genérico de framework.
 */
public record CreateTenantRawRequest(String code, String name) {
}
