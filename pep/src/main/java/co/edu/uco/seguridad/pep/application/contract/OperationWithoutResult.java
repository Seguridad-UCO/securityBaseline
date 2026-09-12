package co.edu.uco.seguridad.pep.application.contract;

/** Regla síncrona, determinista y sin resultado significativo. */
@FunctionalInterface
public interface OperationWithoutResult<I> {
    void execute(I input);
}
