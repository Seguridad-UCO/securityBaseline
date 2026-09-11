package co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * El cuerpo JSON exactamente como llegó al {@code PUT}. {@code userId} no está aquí: llega de la
 * ruta, no del cuerpo.
 */
public record AssignTenantBodyRequest(String tenantCode) {
}
