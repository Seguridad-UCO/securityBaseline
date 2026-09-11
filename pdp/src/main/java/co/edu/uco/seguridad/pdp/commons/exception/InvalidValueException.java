package co.edu.uco.seguridad.pdp.commons.exception;

/**
 * Se pidió a un objeto de valor que representara algo que no puede representar.
 *
 * <p>Generado por constructores y fábricas, nunca por setters: un objeto de valor que existe es
 * siempre válido.</p>
 */
public abstract class InvalidValueException extends DomainException {

    protected InvalidValueException(String code, String message) {
        super(code, message);
    }
}
