package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.repository;

import co.edu.uco.seguridad.shared.audit.AdministrationAuditRepository;
import co.edu.uco.seguridad.shared.audit.AdministrationEvent;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Adaptador real sobre SurrealDB (HU-021), misma forma que {@code SurrealAccessAuditRepository}.
 * Pendiente: SurrealQL literal contra {@code AdministrationEventSchema.TABLE}, vía
 * {@code AdministrationEventPersistenceMapper} — esqueleto dejado por {@code 2-tester-spec}.
 */
public final class SurrealAdministrationAuditRepository implements AdministrationAuditRepository {

    private final SurrealDbClient client;

    public SurrealAdministrationAuditRepository(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, RequiredArgumentMessages.SURREALDB_CLIENT);
    }

    @Override
    public Mono<Void> save(AdministrationEvent event) {
        throw new UnsupportedOperationException("pendiente: HU-021");
    }

    @Override
    public Flux<AdministrationEvent> findByCorrelationId(String correlationId) {
        throw new UnsupportedOperationException("pendiente: HU-021");
    }
}
