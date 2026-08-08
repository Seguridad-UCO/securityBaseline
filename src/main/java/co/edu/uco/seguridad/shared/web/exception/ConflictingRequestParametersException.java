package co.edu.uco.seguridad.shared.web.exception;

/**
 * Los parámetros son individualmente válidos pero no se pueden combinar.
 *
 * <p>Se usa para rechazar ventanas de resultado ambiguas — {@code offset} sin {@code limit}, o paginación
 * e intervalos a la vez — en lugar de elegir silenciosamente una interpretación.</p>
 */
public final class ConflictingRequestParametersException extends RequestContractException {

    public ConflictingRequestParametersException(String field, String reason) {
        super("CONFLICTING_REQUEST_PARAMETERS", field, reason);
    }

}
