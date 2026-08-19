package co.edu.uco.seguridad.pdp.resources.application.port.secondary.repository;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.PageWindow;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.commons.ResultPage;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.resources.domain.ActionCode;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedApplicationCriteria;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.ResourceCode;
import reactor.core.publisher.Mono;

/**
 * Puerto secundario para almacenamiento de catálogo (criterio 16).
 *
 * <p>Las lecturas pasan a través de un único {@code findBy(criteria, window)} en lugar de un método por
 * filtro, por lo que un nuevo filtro es un nuevo campo en el objeto de criterios y no una nueva API aquí.</p>
 */
public interface ProtectedResourceRepository {

    /**
     * {@code tenantId} es obligatorio aunque {@code applicationId} ya identifique una aplicación de un
     * tenant concreto: ningún método de este puerto sobre datos con dueño debería poder invocarse sin
     * tenant — es la propiedad que hace que el aislamiento multi-tenant sea del adaptador, no una
     * consecuencia de que todos los llamadores actuales se porten bien.
     */
    Mono<Boolean> existsGrant(TenantId tenantId, ApplicationId applicationId, ResourceCode resourceCode,
            ActionCode action);

    Mono<ResultPage<ProtectedResource>> findBy(ProtectedApplicationCriteria criteria, PageWindow window);

    Mono<ProtectedResource> save(ProtectedResource resource);

    Mono<Void> deleteById(ResourceId resourceId);
}
