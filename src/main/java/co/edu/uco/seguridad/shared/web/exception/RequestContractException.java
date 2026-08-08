package co.edu.uco.seguridad.shared.web.exception;

import co.edu.uco.seguridad.crosscutting.messages.WebContractMessages;

import java.util.Objects;

/**
 * La carga entrante no honra el contrato de solicitud.
 *
 * <p>Deliberadamente no es una {@code DomainException}: se trata de la forma de lo que llegó sobre
 * HTTP, una preocupación que el núcleo ni conoce ni debería aprender. Mantener las dos jerarquías aparte es
 * lo que permite al controlador de errores responder "tu solicitud es malformada" y "tu solicitud es
 * bien formada pero prohibida" de manera diferente.</p>
 *
 * <p>Reemplaza las anotaciones de validación de Jakarta que previamente protegían el límite: las mismas
 * verificaciones ahora se ejecutan como código explícito, después de que Jackson haya terminado de hacer binding,
 * para que un campo inválido sea reportado por esta aplicación en lugar de ser rechazado antes de ser alcanzado.</p>
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
