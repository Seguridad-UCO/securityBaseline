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
}
