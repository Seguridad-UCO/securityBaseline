package co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository;

import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto secundario para almacenamiento de recursos protegidos, expresado solo en tipos de dominio.
 */
public interface ProtectedResourceRepository {

    Mono<Boolean> existsByApplicationPathAndMethod(ApplicationId applicationId, ResourcePath path, HttpVerb method);

    /**
     * True si la aplicación aún conserva recursos protegidos.
     */
    default Mono<Boolean> existsByApplicationId(ApplicationId applicationId) {
        return Mono.just(false);
    }

    Flux<ProtectedResource> findAllByApplication(ApplicationId applicationId);

    /**
     * Página acotada para interfaces administrativas; evita materializar un catálogo completo.
     */
    default Mono<ResultPage<ProtectedResource>> findPageByApplication(ApplicationId applicationId, PageWindow window) {
        return Mono.error(new UnsupportedOperationException("La consulta paginada no está implementada"));
    }
    default Mono<Long> countByApplication(ApplicationId applicationId) { return Mono.error(new UnsupportedOperationException()); }

    Mono<ProtectedResource> save(ProtectedResource resource);

    default Mono<ProtectedResource> findByIdForTenant(ResourceId resourceId, TenantId tenantId) {
        return Mono.empty();
    }

    default Mono<ProtectedResource> update(ProtectedResource resource) {
        return Mono.error(new UnsupportedOperationException());
    }

    Mono<Void> deleteById(ResourceId resourceId);

    /**
     * Resuelve qué aplicación es dueña de un recurso, a partir solo de su identificador —
     * a diferencia de {@link #findAllByApplication}, que ya conoce la aplicación. Vacío si el
     * recurso no existe (HU-004, {@code ProtectedResourceOwnerLookupValidator}).
     */
    Mono<ApplicationId> findApplicationIdById(ResourceId resourceId);

    /**
     * Finds the registered resource covering a concrete request path. A template segment such as
     * {@code {id}} covers exactly one segment of the request path.
     */
    default Mono<ProtectedResource> findByApplicationPathAndMethod(ApplicationId applicationId, ResourcePath path,
                                                                   HttpVerb method) {
        return findAllByApplication(applicationId)
                .filter(resource -> resource.method() == method)
                .filter(resource -> resource.path().matches(path))
                // A literal path is more specific than a template and must always win.
                .sort((left, right) -> Boolean.compare(right.path().equals(path), left.path().equals(path)))
                .next();
    }

    default Mono<ResourceId> findIdByApplicationPathAndMethod(ApplicationId applicationId, ResourcePath path,
                                                               HttpVerb method) {
        return findByApplicationPathAndMethod(applicationId, path, method).map(ProtectedResource::id);
    }
}
