package co.edu.uco.seguridad.pdp.commons.exception;

/**
 * Una regla de negocio rechazó una operación cuyos valores individuales estaban bien formados.
 *
 * <p>Cada regla posee exactamente una subclase para que la razón nunca se infiera de un mensaje.</p>
 */
public abstract class BusinessRuleViolationException extends DomainException {

    protected BusinessRuleViolationException(String code, String message) {
        super(code, message);
    }
}
