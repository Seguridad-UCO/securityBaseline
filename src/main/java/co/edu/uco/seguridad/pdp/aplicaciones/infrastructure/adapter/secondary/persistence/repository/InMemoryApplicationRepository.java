package co.edu.uco.seguridad.pdp.aplicaciones.infrastructure.adapter.secondary.persistence.repository;

import co.edu.uco.seguridad.pdp.aplicaciones.application.port.secondary.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.aplicaciones.domain.Application;
import co.edu.uco.seguridad.pdp.aplicaciones.infrastructure.adapter.secondary.persistence.entity.ApplicationEntity;
import co.edu.uco.seguridad.pdp.aplicaciones.infrastructure.adapter.secondary.persistence.mapper.ApplicationPersistenceMapper;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador secundario (driven) dummy sustituible (criterio 07). Almacena entidades, no objetos de
 * dominio, por lo que reemplazarlo con una base de datos real no cambia nada por encima de esta clase.
 */
public final class InMemoryApplicationRepository implements ApplicationRepository {

    private final Map<String, ApplicationEntity> rows = new ConcurrentHashMap<>();

    @Override
    public Mono<Boolean> existsByTenantAndName(TenantId tenantId, ApplicationName name) {
        return Mono.fromSupplier(() -> rows.values().stream()
                .map(ApplicationPersistenceMapper::toDomain)
                .anyMatch(application -> application.belongsTo(tenantId) && application.isNamed(name)));
    }

    @Override
    public Mono<Application> save(Application application) {
        return Mono.fromSupplier(() -> {
            rows.put(application.id().value().toString(), ApplicationPersistenceMapper.toEntity(application));
            return application;
        });
    }

    @Override
    public Mono<Void> deleteById(ApplicationId applicationId) {
        return Mono.fromRunnable(() -> rows.remove(applicationId.value().toString()));
    }
}
