package co.edu.uco.seguridad.shared.auth.model;

/**
 * La sesión BFF ya no puede entregar un token vigente: no hay cliente autorizado en la sesión, el
 * refresh token no existe o Keycloak lo rechazó ({@code invalid_grant}). El canal interno responde 401.
 * (HU-030, ADR-033 §2, ADR-025).
 */
public final class BffSessionExpiredException extends RuntimeException {

    public BffSessionExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
