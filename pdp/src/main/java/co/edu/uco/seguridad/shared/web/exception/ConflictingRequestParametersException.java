package co.edu.uco.seguridad.shared.web.exception;

/**
 * Los parámetros son individualmente válidos pero no se pueden combinar (p. ej. paginación + rango).
 */
public final class ConflictingRequestParametersException extends RequestContractException {

    public ConflictingRequestParametersException(String field, String reason) {
        super("CONFLICTING_REQUEST_PARAMETERS", field, reason);
    }

}
