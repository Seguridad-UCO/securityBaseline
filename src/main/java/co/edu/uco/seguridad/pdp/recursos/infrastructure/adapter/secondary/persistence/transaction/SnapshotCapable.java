package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.transaction;

/**
 * Capacidad de instantánea que un almacén dummy ofrece para simular una transacción.
 *
 * <p>Ningún puerto de aplicación declara esto — {@code ProtectedResourceRepository} deliberadamente
 * no lo hace, porque una base de datos real gestiona sus transacciones con su propio mecanismo, no
 * con copias en memoria. Esta interfaz existe solo para que {@link SnapshotReactiveTransactionAdapter}
 * dependa de una abstracción en lugar del tipo concreto del almacén dummy; desaparece junto con él
 * cuando la persistencia real implemente {@link co.edu.uco.seguridad.shared.port.ReactiveTransactionPort}
 * con la transacción propia del motor.</p>
 *
 * @param <S> la forma de la instantánea (p. ej. una copia del mapa de filas)
 */
public interface SnapshotCapable<S> {

    S snapshot();

    void restore(S snapshot);
}
