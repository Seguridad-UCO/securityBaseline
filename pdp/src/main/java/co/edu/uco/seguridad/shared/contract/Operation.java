package co.edu.uco.seguridad.shared.contract;

/** Forma genérica sincrónica con entrada y salida, sin E/S — base común para reglas y validadores. */
@FunctionalInterface
public interface Operation<I, O> {

    O execute(I input);
}
