package co.edu.uco.seguridad.shared.web.exception;

import co.edu.uco.seguridad.crosscutting.messages.WebContractMessages;

/**
 * Un campo estaba presente pero no se pudo convertir al tipo que espera la aplicación.
 *
 * <p>La razón viene de cualquier objeto de valor que rechazó el valor, por lo que la regla de formato se
 * declara una sola vez, en el dominio, y esta excepción solo agrega qué campo la llevaba.</p>
 */
public final class MalformedRequestFieldException extends RequestContractException {

    public MalformedRequestFieldException(String field, String reason) {
        super("MALFORMED_REQUEST_FIELD", field, WebContractMessages.malformedField(field, reason));
    }
}
