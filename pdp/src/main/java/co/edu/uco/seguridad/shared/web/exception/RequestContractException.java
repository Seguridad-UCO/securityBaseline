package co.edu.uco.seguridad.shared.web.exception;

import co.edu.uco.seguridad.shared.web.message.WebContractMessages;

import java.util.Objects;

/**
 * La carga entrante no honra el contrato de solicitud. Deliberadamente no es una
 * {@code DomainException}: es la forma de lo que llegó sobre HTTP, algo que el núcleo no debe conocer.
 */
public abstract class RequestContractException extends RuntimeException {

    private final String code;
    private final String field;

    protected RequestContractException(String code, String field, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, WebContractMessages.requireErrorCode());
        this.field = Objects.requireNonNull(field, WebContractMessages.requireOffendingField());
    }

    public String code() {
        return code;
    }

    public String field() {
        return field;
    }
}
