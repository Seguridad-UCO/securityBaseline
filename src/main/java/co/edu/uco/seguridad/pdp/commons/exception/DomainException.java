package co.edu.uco.seguridad.pdp.commons.exception;

import java.util.Objects;

/**
 * Raíz de todo fallo que el núcleo de PDP puede generar.
 *
 * <p>El núcleo nunca conoce HTTP. Solo publica un {@code código} estable que el adaptador de punto de entrada
 * traduce una sola vez en una respuesta específica del transporte.</p>
 */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(Objects.requireNonNull(message, "se requiere mensaje de excepción de dominio"));
        this.code = Objects.requireNonNull(code, "se requiere código de excepción de dominio");
    }

    public String code() {
        return code;
    }
}
