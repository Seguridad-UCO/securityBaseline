package co.edu.uco.seguridad.shared.web.exception;

import co.edu.uco.seguridad.crosscutting.messages.WebContractMessages;

/**
 * Un campo requerido estaba ausente, nulo o en blanco.
 */
public final class MissingRequestFieldException extends RequestContractException {

    public MissingRequestFieldException(String field) {
        super("MISSING_REQUEST_FIELD", field, WebContractMessages.missingField(field));
    }
}
