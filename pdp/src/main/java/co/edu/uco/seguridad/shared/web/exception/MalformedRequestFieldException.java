package co.edu.uco.seguridad.shared.web.exception;

import co.edu.uco.seguridad.shared.web.message.WebContractMessages;

/**
 * Un campo estaba presente pero no se pudo convertir al tipo esperado.
 */
public final class MalformedRequestFieldException extends RequestContractException {

    public MalformedRequestFieldException(String field, String reason) {
        super("MALFORMED_REQUEST_FIELD", field, WebContractMessages.malformedField(field, reason));
    }
}
