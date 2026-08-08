package co.edu.uco.seguridad.pdp.aplicaciones.infrastructure.adapter.secondary.persistence.repository;

import co.edu.uco.seguridad.pdp.aplicaciones.application.port.secondary.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.aplicaciones.domain.Application;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

/**
 * Adaptador secundario (driven) real sobre SurrealDB (ADR-0004). Habla en tipos de dominio; la
 * fila SurrealDB (id, tenantId, name, registeredAt) es un detalle interno de esta clase.
 *
 * <p>{@code existsByTenantAndName} sigue siendo la puerta principal contra duplicados —
 * {@code application_tenant_name} (ver {@link SurrealApplicationSchemaInitializer}) es un respaldo
 * a nivel de almacén contra la ventana de carrera entre el chequeo y la escritura, no el mecanismo
 * que produce el mensaje de error que ve el cliente.</p>
 */
public final class SurrealApplicationRepository implements ApplicationRepository {

    private final SurrealDbClient client;

    public SurrealApplicationRepository(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, "se requiere el cliente de SurrealDB");
    }

    @Override
    public Mono<Boolean> existsByTenantAndName(TenantId tenantId, ApplicationName name) {
        return client.execute(
                        "SELECT id FROM application WHERE tenantId = $tenantId AND name = $name LIMIT 1;",
                        Map.of("tenantId", tenantId.value(), "name", name.value()))
                .map(results -> !results.get(0).isEmpty());
    }

    @Override
    public Mono<Application> save(Application application) {
        return client.execute(
                        """
                        CREATE type::record('application', $id) SET \
                        tenantId = $tenantId, name = $name, registeredAt = <datetime>$registeredAt;\
                        """,
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
                        "DELETE type::record('application', $id);",
                        Map.of("id", applicationId.value().toString()))
                .then();
    }
}
