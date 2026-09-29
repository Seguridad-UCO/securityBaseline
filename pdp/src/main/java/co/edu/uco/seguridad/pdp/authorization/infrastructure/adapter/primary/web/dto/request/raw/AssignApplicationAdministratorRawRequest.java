package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * El applicationId lo pone el controller desde la ruta; userId viene en el cuerpo (HU-020).
 */
public record AssignApplicationAdministratorRawRequest(String applicationId, String userId) {
}
