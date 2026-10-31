package co.edu.uco.seguridad.shared.auth.model;

/**
 * No se pudo renovar el token porque Keycloak no respondió a tiempo o respondió con un error que no
 * prueba que la sesión murió. El canal interno responde 503; jamás se entrega un token vencido
 * (HU-030, ADR-025).
 */
public final class TokenRenewalUnavailableException extends RuntimeException {

    public TokenRenewalUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
