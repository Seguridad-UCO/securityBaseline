package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.transaction;

import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.entity.ProtectedResourceEntity;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.repository.InMemoryProtectedResourceRepository;
import co.edu.uco.seguridad.shared.port.ReactiveTransactionPort;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Adaptador secundario (driven) de transacción dummy: copia el almacén antes de que se ejecute el
 * trabajo y devuelve la copia si el trabajo falla.
 *
 * <p>{@code Mono.defer} es lo que hace correcta la instantánea — se toma en el momento de la suscripción,
 * no cuando se ensambla el pipeline. El rollback es demostrativo y específico del almacén en memoria;
 * un adaptador real implementará el mismo puerto con la propia transacción del motor, y nada por encima
 * de esta clase cambia.</p>
 */
public final class SnapshotReactiveTransactionAdapter implements ReactiveTransactionPort {

    private final InMemoryProtectedResourceRepository repository;

    public SnapshotReactiveTransactionAdapter(InMemoryProtectedResourceRepository repository) {
        this.repository = Objects.requireNonNull(repository, "se requiere repositorio de recurso protegido");
    }

    @Override
    public <T> Mono<T> execute(Supplier<Mono<T>> work) {
        return Mono.defer(() -> {
            Map<String, ProtectedResourceEntity> snapshot = repository.snapshot();
            return work.get().onErrorResume(error -> {
                repository.restore(snapshot);
                return Mono.error(error);
            });
        });
    }
}
