package co.edu.uco.seguridad.applications.infrastructure.persistence.dummy;

import co.edu.uco.seguridad.applications.application.port.out.*;
import co.edu.uco.seguridad.applications.domain.*;
import reactor.core.publisher.Mono;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** Deliberately replaceable dummy adapter; it is not a production persistence implementation. */
public final class InMemoryProtectedApplicationRepository implements ProtectedApplicationRepository, Snapshotable {
    private final Map<ProtectedApplicationId, ProtectedApplication> data = new ConcurrentHashMap<>();
    public Mono<Boolean> existsByTenantAndName(TenantId tenant, ApplicationName name) {
        return Mono.fromSupplier(() -> data.values().stream().anyMatch(a -> a.tenantId().equals(tenant) && a.name().value().equalsIgnoreCase(name.value())));
    }
    public Mono<ProtectedApplication> save(ProtectedApplication application) { return Mono.fromSupplier(() -> { data.put(application.id(), application); return application; }); }
    public Mono<ApplicationPage<ProtectedApplication>> findBy(ProtectedApplicationCriteria criteria, PageWindow window) {
        return Mono.fromSupplier(() -> {
            List<ProtectedApplication> all = data.values().stream().filter(criteria::matches)
                .sorted(Comparator.comparing(ProtectedApplication::registeredAt).reversed()).toList();
            List<ProtectedApplication> content = all.stream().skip(window.offset()).limit(window.limit()).toList();
            return new ApplicationPage<>(content, all.size(), window.offset(), window.limit());
        });
    }
    public synchronized Object snapshot() { return new HashMap<>(data); }
    @SuppressWarnings("unchecked") public synchronized void restore(Object snapshot) { data.clear(); data.putAll((Map<ProtectedApplicationId, ProtectedApplication>) snapshot); }
}
