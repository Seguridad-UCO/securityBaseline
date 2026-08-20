package co.edu.uco.seguridad.pdp.applications.application.port.secondary.repository;

import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto secundario para almacenamiento de aplicaciones, expresado solo en tipos de dominio.
 *
 * <p>La existencia es una pregunta de primera clase aquí en lugar de un {@code findByName} que el
 * llamador tendría que interpretar, así que el adaptador puede responderla con un índice en lugar de cargar una fila.</p>
 */
public interface ApplicationRepository {

    Mono<Boolean> existsByTenantAndName(TenantId tenantId, ApplicationName name);

    Mono<Application> findByTenantAndId(TenantId tenantId, ApplicationId applicationId);

    Flux<Application> findAllByTenant(TenantId tenantId);

    Mono<Application> save(Application application);

    Mono<Void> deleteById(ApplicationId applicationId);
}
