package co.edu.uco.seguridad.pdp.commons.exception;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import java.util.Objects;

/**
 * Raíz de to-do fallo que el núcleo de PDP puede generar.
 *
 * <p>El núcleo nunca conoce HTTP. Solo publica un {@code código} estable que el adaptador de punto de entrada
 * traduce una sola vez en una respuesta específica del transporte.</p>
 */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(Objects.requireNonNull(message, RequiredArgumentMessages.DOMAIN_EXCEPTION_MESSAGE));
        this.code = Objects.requireNonNull(code, RequiredArgumentMessages.DOMAIN_EXCEPTION_CODE);
    }

    public String code() {
        return code;
    }
}
