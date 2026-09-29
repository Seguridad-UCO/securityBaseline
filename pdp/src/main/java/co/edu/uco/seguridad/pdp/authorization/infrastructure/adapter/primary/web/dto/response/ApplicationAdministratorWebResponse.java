package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response;

/**
 * Aplanado web de un administrador activo de una aplicación (HU-020).
 */
public record ApplicationAdministratorWebResponse(String userId, String validFrom, String validUntil) {
}
