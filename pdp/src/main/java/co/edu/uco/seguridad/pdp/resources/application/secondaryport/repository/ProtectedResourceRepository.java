package co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Puerto secundario para almacenamiento de recursos protegidos, expresado solo en tipos de dominio. */
public interface ProtectedResourceRepository {

    Mono<Boolean> existsByApplicationPathAndMethod(ApplicationId applicationId, ResourcePath path, HttpVerb method);

    Flux<ProtectedResource> findAllByApplication(ApplicationId applicationId);

    Mono<ProtectedResource> save(ProtectedResource resource);

    Mono<Void> deleteById(ResourceId resourceId);

    /**
     * Resuelve qué aplicación es dueña de un recurso, a partir solo de su identificador —
     * a diferencia de {@link #findAllByApplication}, que ya conoce la aplicación. Vacío si el
     * recurso no existe (HU-004, {@code ProtectedResourceOwnerLookupValidator}).
     */
    Mono<ApplicationId> findApplicationIdById(ResourceId resourceId);
}
