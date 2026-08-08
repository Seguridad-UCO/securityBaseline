package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.repository;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.PageWindow;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.commons.ResultPage;
import co.edu.uco.seguridad.pdp.recursos.application.port.secondary.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.recursos.domain.ActionCode;
import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedApplicationCriteria;
import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.recursos.domain.ResourceCode;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.entity.ProtectedResourceEntity;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.mapper.ProtectedResourcePersistenceMapper;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador secundario (driven) sustituible (criterio 07).
 *
 * <p>Ejecuta la especificación que se le entrega y no decide nada: {@code matches} pertenece
 * al dominio, y esta clase solo elige <em>cómo</em> ejecutarla — un escaneo completo aquí, un índice o
 * una cláusula {@code WHERE} en una base de datos real. El orden se fija por el instante de registro y luego
 * por id, para que la paginación sea estable en lugar de depender del orden del hash.</p>
 */
public final class InMemoryProtectedResourceRepository implements ProtectedResourceRepository {

    private static final Comparator<ProtectedResource> STABLE_ORDER =
            Comparator.comparing(ProtectedResource::registeredAt)
                    .thenComparing(resource -> resource.id().value());

    private final Map<String, ProtectedResourceEntity> rows = new ConcurrentHashMap<>();

    @Override
    public Mono<Boolean> existsGrant(ApplicationId applicationId, ResourceCode resourceCode, ActionCode action) {
        return Mono.fromSupplier(() -> readAll().stream()
                .anyMatch(resource -> resource.isSameGrantAs(applicationId, resourceCode, action)));
    }

    @Override
    public Mono<ResultPage<ProtectedResource>> findBy(ProtectedApplicationCriteria criteria, PageWindow window) {
        return Mono.fromSupplier(() -> {
            List<ProtectedResource> matches = readAll().stream()
                    .filter(criteria::matches)
                    .sorted(STABLE_ORDER)
                    .toList();
            List<ProtectedResource> content = matches.stream()
                    .skip(window.offset())
                    .limit(window.limit())
                    .toList();
            return ResultPage.of(content, matches.size(), window);
        });
    }

    @Override
    public Mono<ProtectedResource> save(ProtectedResource resource) {
        return Mono.fromSupplier(() -> {
            rows.put(resource.id().value().toString(), ProtectedResourcePersistenceMapper.toEntity(resource));
            return resource;
        });
    }

    @Override
    public Mono<Void> deleteById(ResourceId resourceId) {
        return Mono.fromRunnable(() -> rows.remove(resourceId.value().toString()));
    }

    private List<ProtectedResource> readAll() {
        return rows.values().stream().map(ProtectedResourcePersistenceMapper::toDomain).toList();
    }

    /**
     * Visible solo para {@link co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.transaction.SnapshotReactiveTransactionAdapter};
     * el puerto nunca expone esto.
     */
    public Map<String, ProtectedResourceEntity> snapshot() {
        return new LinkedHashMap<>(rows);
    }

    public void restore(Map<String, ProtectedResourceEntity> snapshot) {
        rows.clear();
        rows.putAll(snapshot);
    }
}
