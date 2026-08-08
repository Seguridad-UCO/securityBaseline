package co.edu.uco.seguridad.pdp.recursos.application.port.secondary.repository;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.PageWindow;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.commons.ResultPage;
import co.edu.uco.seguridad.pdp.recursos.domain.ActionCode;
import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedApplicationCriteria;
import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.recursos.domain.ResourceCode;
import reactor.core.publisher.Mono;

/**
 * Puerto secundario para almacenamiento de catálogo (criterio 16).
 *
 * <p>Las lecturas pasan a través de un único {@code findBy(criteria, window)} en lugar de un método por
 * filtro, por lo que un nuevo filtro es un nuevo campo en el objeto de criterios y no una nueva API aquí.</p>
 */
public interface ProtectedResourceRepository {

    Mono<Boolean> existsGrant(ApplicationId applicationId, ResourceCode resourceCode, ActionCode action);

    Mono<ResultPage<ProtectedResource>> findBy(ProtectedApplicationCriteria criteria, PageWindow window);

    Mono<ProtectedResource> save(ProtectedResource resource);

    Mono<Void> deleteById(ResourceId resourceId);
}
