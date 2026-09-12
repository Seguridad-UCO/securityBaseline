package co.edu.uco.seguridad.pdp.commons.exception;

/**
 * Violación de regla cuyo significado HTTP es conflicto (recurso ya existente).
 *
 * <p>El manejador de errores traduce cualquier subclase a 409 sin conocer excepciones concretas
 * de cada módulo.</p>
 */
public abstract class ConflictBusinessRuleException extends BusinessRuleViolationException {

    protected ConflictBusinessRuleException(String code, String message) {
        super(code, message);
    }
}
