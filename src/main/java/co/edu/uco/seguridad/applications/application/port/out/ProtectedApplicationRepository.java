package co.edu.uco.seguridad.applications.application.port.out;

import co.edu.uco.seguridad.applications.domain.*;
import reactor.core.publisher.Mono;

public interface ProtectedApplicationRepository {
    Mono<Boolean> existsByTenantAndName(TenantId tenantId, ApplicationName name);
    Mono<ProtectedApplication> save(ProtectedApplication application);
    Mono<ApplicationPage<ProtectedApplication>> findBy(ProtectedApplicationCriteria criteria, PageWindow window);
}
