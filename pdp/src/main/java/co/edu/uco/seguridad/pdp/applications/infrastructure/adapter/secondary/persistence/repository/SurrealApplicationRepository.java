package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.secondary.persistence.repository;

import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.ApplicationCriteria;
import co.edu.uco.seguridad.pdp.applications.domain.exception.AmbiguousApplicationNameException;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationCredentialHash;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.secondary.persistence.entity.ApplicationEntity;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.secondary.persistence.mapper.ApplicationPersistenceMapper;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.secondary.persistence.schema.ApplicationSchema;
import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealRecordId;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.util.*;

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
    public Mono<Boolean> existsByTenantAndId(TenantId tenantId, ApplicationId applicationId) {
        return client.execute(
                        "SELECT id FROM type::record('%s', $id) WHERE tenantId = $tenantId;"
                                .formatted(ApplicationSchema.TABLE),
                        Map.of("id", applicationId.value().toString(), "tenantId", tenantId.value()))
                .map(results -> !results.get(0).isEmpty());
    }

    @Override
    public Mono<TenantId> findTenantIdById(ApplicationId applicationId) {
        return client.execute(
                        "SELECT tenantId FROM type::record('%s', $id);".formatted(ApplicationSchema.TABLE),
                        Map.of("id", applicationId.value().toString()))
                .flatMap(results -> {
                    JsonNode rows = results.get(0);
                    return rows.isEmpty() ? Mono.empty() : Mono.just(new TenantId(rows.get(0).path("tenantId").asString()));
                });
    }

    @Override
    public Mono<Application> findUniqueByName(ApplicationName name) {
        return client.execute(
                        "SELECT * FROM %s WHERE string::lowercase(name) = string::lowercase($name) LIMIT 2;"
                                .formatted(ApplicationSchema.TABLE),
                        Map.of("name", name.value()))
                .flatMap(results -> {
                    JsonNode rows = results.get(0);
                    if (rows.isEmpty()) return Mono.empty();
                    if (rows.size() > 1) return Mono.error(new AmbiguousApplicationNameException(name));
                    return Mono.just(toDomain(rows.get(0)));
                });
    }

    @Override
    public Mono<Application> findByTenantAndName(TenantId tenantId, ApplicationName name) {
        return client.execute("SELECT * FROM %s WHERE tenantId = $tenantId AND string::lowercase(name) = string::lowercase($name) LIMIT 1;"
                        .formatted(ApplicationSchema.TABLE), Map.of("tenantId", tenantId.value(), "name", name.value()))
                .flatMap(results -> results.get(0).isEmpty() ? Mono.empty() : Mono.just(toDomain(results.get(0).get(0))));
    }

    @Override
    public Mono<ApplicationCredentialHash> findCredentialHashById(ApplicationId applicationId) {
        return client.execute(
                        "SELECT credentialHash FROM type::record('%s', $id);".formatted(ApplicationSchema.TABLE),
                        Map.of("id", applicationId.value().toString()))
                .flatMap(results -> {
                    JsonNode rows = results.get(0);
                    return rows.isEmpty()
                            ? Mono.empty()
                            : Mono.just(new ApplicationCredentialHash(rows.get(0).path("credentialHash").asString()));
                });
    }

    /**
     * Traduce la specification a una consulta: el filtro opcional se añade al {@code WHERE} solo si
     * el criterio lo trae, y el recorte va en la propia consulta, no en memoria — si el puerto
     * trajese el catálogo entero, la paginación sería cosmética.
     *
     * <p>Dos sentencias en una sola llamada: la página y el total del filtro completo. El total es
     * el del criterio, no el de la página, que es lo que el cliente necesita para navegar.
     */
    @Override
    public Mono<ResultPage<Application>> findBy(ApplicationCriteria criteria, PageWindow window) {
        String filter = criteria.nameContains()
                .map(fragment -> " AND string::contains(string::lowercase(name), $name)")
                .orElse("");

        Map<String, String> parameters = new HashMap<>();
        parameters.put("tenantId", criteria.tenantId().value());
        criteria.nameContains().ifPresent(fragment -> parameters.put("name", fragment.toLowerCase(Locale.ROOT)));

        // El cliente solo acepta parámetros de texto, y LIMIT/START exigen números. Interpolarlos es
        // seguro aquí y solo aquí: no son texto del usuario, son dos int que PageWindow ya validó
        // (offset >= 0, 1 <= limit <= 100). El filtro de nombre, que sí viene del usuario, va como
        // parámetro ligado.
        String query = """
                SELECT * FROM %s WHERE tenantId = $tenantId%s                 ORDER BY registeredAt DESC LIMIT %d START %d;
                SELECT count() FROM %s WHERE tenantId = $tenantId%s GROUP ALL;                """.formatted(ApplicationSchema.TABLE, filter, window.limit(), window.offset(),
                ApplicationSchema.TABLE, filter);

        return client.execute(query, parameters)
                .map(results -> {
                    List<Application> content = results.get(0).valueStream()
                            .map(SurrealApplicationRepository::toDomain)
                            .toList();
                    return ResultPage.of(content, totalOf(results.get(1)), window);
                });
    }

    private static long totalOf(JsonNode countResult) {
        if (countResult.isEmpty()) {
            return 0L;
        }
        return countResult.get(0).path("count").asLong(0L);
    }

    @Override
    public Mono<Application> save(Application application) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("id", application.id().value().toString());
        parameters.put("tenantId", application.tenantId().value());
        parameters.put("name", application.name().value());
        parameters.put("description", application.description());
        parameters.put("baseUrl", application.baseUrl().value());
        parameters.put("credentialHash", application.credentialHash().value());
        parameters.put("enabledEnvironments", application.enabledEnvironments().stream().map(environment -> environment.value()).sorted().collect(java.util.stream.Collectors.joining(",")));
        parameters.put("registeredAt", application.registeredAt().toString());
        return client.execute(
                        """
                                CREATE type::record('%s', $id) SET \
                                tenantId = $tenantId, name = $name, description = $description, baseUrl = $baseUrl, \
                                credentialHash = $credentialHash, enabledEnvironments = $enabledEnvironments, registeredAt = <datetime>$registeredAt;\
                                """.formatted(ApplicationSchema.TABLE),
                        parameters)
                .thenReturn(application);
    }

    @Override
    public Mono<Application> findByIdForTenant(TenantId tenantId, ApplicationId applicationId) {
        return client.execute(
                        "SELECT * FROM type::record('%s', $id) WHERE tenantId = $tenantId;"
                                .formatted(ApplicationSchema.TABLE),
                        Map.of("id", applicationId.value().toString(), "tenantId", tenantId.value()))
                .flatMap(results -> {
                    JsonNode rows = results.get(0);
                    return rows.isEmpty() ? Mono.empty() : Mono.just(toDomain(rows.get(0)));
                });
    }

    @Override
    public Mono<Application> update(Application application) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("id", application.id().value().toString());
        parameters.put("name", application.name().value());
        parameters.put("description", application.description());
        parameters.put("baseUrl", application.baseUrl().value());
        parameters.put("enabledEnvironments", application.enabledEnvironments().stream().map(environment -> environment.value()).sorted().collect(java.util.stream.Collectors.joining(",")));
        return client.execute(
                        "UPDATE type::record('%s', $id) SET name = $name, description = $description, baseUrl = $baseUrl, enabledEnvironments = $enabledEnvironments;"
                                .formatted(ApplicationSchema.TABLE),
                        parameters)
                .thenReturn(application);
    }

    @Override
    public Mono<Void> updateCredentialHash(ApplicationId applicationId, ApplicationCredentialHash credentialHash) {
        return client.execute(
                        "UPDATE type::record('%s', $id) SET credentialHash = $credentialHash;"
                                .formatted(ApplicationSchema.TABLE),
                        Map.of("id", applicationId.value().toString(), "credentialHash", credentialHash.value()))
                .then();
    }

    @Override
    public Mono<Void> deleteById(ApplicationId applicationId) {
        return client.execute(
                        "DELETE type::record('%s', $id);".formatted(ApplicationSchema.TABLE),
                        Map.of("id", applicationId.value().toString()))
                .then();
    }

    private static Application toDomain(JsonNode row) {
        ApplicationEntity entity = new ApplicationEntity(
                SurrealRecordId.idPart(row.path("id").asString()),
                row.path("tenantId").asString(),
                row.path("name").asString(),
                row.path("description").asString(""),
                row.path("baseUrl").asString(),
                row.path("credentialHash").asString(),
                row.path("registeredAt").asString(),
                java.util.Arrays.stream(row.path("enabledEnvironments").asString("").split(","))
                        .filter(value -> !value.isBlank()).toList());
        return ApplicationPersistenceMapper.toDomain(entity);
    }
}
