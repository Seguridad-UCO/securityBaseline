package co.edu.uco.seguridad.pdp.recursos.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.PageWindow;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.commons.ResultPage;
import co.edu.uco.seguridad.pdp.recursos.application.port.secondary.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.recursos.domain.ActionCode;
import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedApplicationCriteria;
import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.recursos.domain.ResourceCode;
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
final class FakeProtectedResourceRepository implements ProtectedResourceRepository {

    private final Map<String, ProtectedResource> rows = new ConcurrentHashMap<>();

    @Override
    public Mono<Boolean> existsGrant(ApplicationId applicationId, ResourceCode resourceCode, ActionCode action) {
        return Mono.fromSupplier(() -> rows.values().stream()
                .anyMatch(resource -> resource.isSameGrantAs(applicationId, resourceCode, action)));
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
        return Mono.fromRunnable(() -> rows.remove(resourceId.value().toString()));
    }
}
