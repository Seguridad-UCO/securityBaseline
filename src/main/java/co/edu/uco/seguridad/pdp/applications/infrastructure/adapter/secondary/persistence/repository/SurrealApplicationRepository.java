package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.secondary.persistence.repository;

import co.edu.uco.seguridad.pdp.applications.application.port.secondary.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.secondary.persistence.schema.ApplicationSchema;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

/**
 * Adaptador real sobre SurrealDB (ADR-019). {@code existsByTenantAndName} es la puerta principal
 * contra duplicados; el índice {@code application_tenant_name} solo cierra la ventana de carrera
 * entre el chequeo y la escritura, no el mecanismo que produce el mensaje de error que ve el cliente.
 */
public final class SurrealApplicationRepository implements ApplicationRepository {

    private final SurrealDbClient client;

    public SurrealApplicationRepository(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, RequiredArgumentMessages.SURREALDB_CLIENT);
    }

    @Override
    public Mono<Boolean> existsByTenantAndName(TenantId tenantId, ApplicationName name) {
        return client.execute(
                        "SELECT id FROM %s WHERE tenantId = $tenantId AND name = $name LIMIT 1;"
                                .formatted(ApplicationSchema.TABLE),
                        Map.of("tenantId", tenantId.value(), "name", name.value()))
                .map(results -> !results.get(0).isEmpty());
    }

    @Override
    public Mono<Application> save(Application application) {
        return client.execute(
                        """
                        CREATE type::record('%s', $id) SET \
                        tenantId = $tenantId, name = $name, registeredAt = <datetime>$registeredAt;\
                        """.formatted(ApplicationSchema.TABLE),
                        Map.of(
                                "id", application.id().value().toString(),
                                "tenantId", application.tenantId().value(),
                                "name", application.name().value(),
                                "registeredAt", application.registeredAt().toString()))
                .thenReturn(application);
    }

    @Override
    public Mono<Void> deleteById(ApplicationId applicationId) {
        return client.execute(
                        "DELETE type::record('%s', $id);".formatted(ApplicationSchema.TABLE),
                        Map.of("id", applicationId.value().toString()))
                .then();
    }
}
