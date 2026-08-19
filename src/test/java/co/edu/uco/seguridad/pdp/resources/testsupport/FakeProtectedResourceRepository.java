package co.edu.uco.seguridad.pdp.resources.testsupport;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.PageWindow;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.commons.ResultPage;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.resources.application.port.secondary.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.domain.ActionCode;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedApplicationCriteria;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.ResourceCode;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Doble de prueba, no un adaptador de producción: desde ADR-0004 el almacén real es SurrealDB
 * ({@code SurrealProtectedResourceRepository}), pero las pruebas de orquestación del caso de uso
 * siguen queriendo correr sin Spring ni una base de datos real — este es su colaborador en memoria.
 */
public final class FakeProtectedResourceRepository implements ProtectedResourceRepository {

    private final Map<String, ProtectedResource> rows = new ConcurrentHashMap<>();
    private RuntimeException deleteFailure;

    /** Para probar que un fallo de compensación no reemplaza el error original que la disparó. */
    public void failDeleteWith(RuntimeException error) {
        this.deleteFailure = error;
    }

    @Override
    public Mono<Boolean> existsGrant(TenantId tenantId, ApplicationId applicationId, ResourceCode resourceCode,
            ActionCode action) {
        return Mono.fromSupplier(() -> rows.values().stream()
                .anyMatch(resource -> resource.belongsTo(tenantId)
                        && resource.isSameGrantAs(applicationId, resourceCode, action)));
    }

    @Override
    public Mono<ResultPage<ProtectedResource>> findBy(ProtectedApplicationCriteria criteria, PageWindow window) {
        return Mono.fromSupplier(() -> {
            List<ProtectedResource> matches = rows.values().stream()
                    .filter(criteria::matches)
                    .sorted(Comparator.comparing(ProtectedResource::registeredAt)
                            .thenComparing(resource -> resource.id().value()))
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
            rows.put(resource.id().value().toString(), resource);
            return resource;
        });
    }

    @Override
    public Mono<Void> deleteById(ResourceId resourceId) {
        if (deleteFailure != null) {
            return Mono.error(deleteFailure);
        }
        return Mono.fromRunnable(() -> rows.remove(resourceId.value().toString()));
    }
}
