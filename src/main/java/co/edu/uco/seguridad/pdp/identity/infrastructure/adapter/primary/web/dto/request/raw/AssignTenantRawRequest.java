package co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * Nivel uno de la estrategia de entrada en dos niveles. {@code userId} llega de la ruta, no del
 * cuerpo, pero viaja junto al resto porque el interactor solo recibe un valor de entrada.
 */
public record AssignTenantRawRequest(String userId, String tenantCode) {
}
