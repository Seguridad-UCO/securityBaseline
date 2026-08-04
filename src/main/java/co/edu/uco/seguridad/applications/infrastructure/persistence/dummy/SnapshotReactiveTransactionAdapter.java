package co.edu.uco.seguridad.applications.infrastructure.persistence.dummy;

import co.edu.uco.seguridad.applications.application.port.out.ReactiveTransactionPort;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.function.Supplier;

/** Educational transaction adapter: rolls back registered in-memory stores when downstream work fails. */
public final class SnapshotReactiveTransactionAdapter implements ReactiveTransactionPort {
    private final List<Snapshotable> stores;
    public SnapshotReactiveTransactionAdapter(List<Snapshotable> stores) { this.stores = List.copyOf(stores); }
    public <T> Mono<T> execute(Supplier<Mono<T>> work) {
        return Mono.defer(() -> {
            var snapshots = stores.stream().map(store -> new Entry(store, store.snapshot())).toList();
            return work.get().onErrorMap(error -> { snapshots.forEach(entry -> entry.store.restore(entry.snapshot)); return error; });
        });
    }
    private record Entry(Snapshotable store, Object snapshot) { }
}
