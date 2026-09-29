package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response;

/**
 * Aplanado web de un administrador activo de una aplicación (HU-020).
 */
public record ApplicationAdministratorWebResponse(ApplicationSecurityUserWebResponse user, String validFrom, String validUntil) {
    /** Compatibility constructor; current application-scoped reads always populate identity details. */
    public ApplicationAdministratorWebResponse(String userId, String validFrom, String validUntil) {
        this(new ApplicationSecurityUserWebResponse(userId, "", ""), validFrom, validUntil);
    }
    /** Internal compatibility for legacy writers; never render this value in administrative views. */
    public String userId() { return user.id(); }
}
