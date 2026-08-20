package co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.secondary.persistence.repository;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.identity.application.port.secondary.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.domain.Email;
import co.edu.uco.seguridad.pdp.identity.domain.ExternalIdentity;
import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import co.edu.uco.seguridad.pdp.identity.domain.UserId;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.secondary.persistence.schema.IdentitySchema;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealRecordId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Adaptador secundario (driven) real sobre SurrealDB (ADR-0004). */
public final class SurrealSecurityUserRepository implements SecurityUserRepository {

    private static final String DEFAULT_PROVIDER = "keycloak-local";

    private final SurrealDbClient client;

    public SurrealSecurityUserRepository(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, RequiredArgumentMessages.SURREALDB_CLIENT);
    }

    @Override
    public Mono<ExternalIdentity> findIdentity(String issuer, String subject) {
        return client.execute(
                        "SELECT * FROM %s WHERE issuer = $issuer AND subject = $subject LIMIT 1;"
                                .formatted(IdentitySchema.IDENTITY_TABLE),
                        Map.of("issuer", issuer, "subject", subject))
                .map(results -> results.get(0))
                .flatMap(rows -> rows.isEmpty() ? Mono.empty() : Mono.just(toIdentity(rows.get(0))));
    }

    @Override
    public Mono<SecurityUser> findByEmail(Email email) {
        return client.execute(
                        "SELECT * FROM %s WHERE email = $email LIMIT 1;".formatted(IdentitySchema.USER_TABLE),
                        Map.of("email", email.value()))
                .map(results -> results.get(0))
                .flatMap(rows -> rows.isEmpty() ? Mono.empty() : Mono.just(toUser(rows.get(0))));
    }

    @Override
    public Mono<SecurityUser> findById(UserId userId) {
        return client.execute(
                        "SELECT * FROM type::record('%s', $id);".formatted(IdentitySchema.USER_TABLE),
                        Map.of("id", userId.value().toString()))
                .map(results -> results.get(0))
                .flatMap(rows -> rows.isEmpty() ? Mono.empty() : Mono.just(toUser(rows.get(0))));
    }

    @Override
    public Mono<String> providerFor(UserId userId) {
        return client.execute(
                        "SELECT provider FROM %s WHERE userId = $userId LIMIT 1;".formatted(IdentitySchema.IDENTITY_TABLE),
                        Map.of("userId", userId.value().toString()))
                .map(results -> results.get(0).isEmpty()
                        ? DEFAULT_PROVIDER
                        : results.get(0).get(0).path("provider").asString(DEFAULT_PROVIDER));
    }

    @Override
    public Mono<SecurityUser> save(SecurityUser user) {
        return client.execute(
                        """
                        UPSERT type::record('%s', $id) SET \
                        email = $email, name = $name, tenantId = $tenantId, \
                        createdAt = <datetime>$createdAt, lastLoginAt = <datetime>$lastLoginAt;\
                        """.formatted(IdentitySchema.USER_TABLE),
                        Map.of(
                                "id", user.id().value().toString(),
                                "email", user.email().value(),
                                "name", user.name(),
                                "tenantId", user.tenantId().value(),
                                "createdAt", user.createdAt().toString(),
                                "lastLoginAt", user.lastLoginAt().toString()))
                .thenReturn(user);
    }

    @Override
    public Mono<Void> linkIdentity(ExternalIdentity identity) {
        return client.execute(
                        """
                        CREATE type::record('%s', $id) SET \
                        userId = $userId, issuer = $issuer, subject = $subject, provider = $provider;\
                        """.formatted(IdentitySchema.IDENTITY_TABLE),
                        Map.of(
                                "id", UUID.randomUUID().toString(),
                                "userId", identity.userId().value().toString(),
                                "issuer", identity.issuer(),
                                "subject", identity.subject(),
                                "provider", identity.provider()))
                .then();
    }

    @Override
    public Flux<SecurityUser> findAll() {
        return client.execute("SELECT * FROM %s ORDER BY lastLoginAt DESC;".formatted(IdentitySchema.USER_TABLE), Map.of())
                .flatMapMany(results -> Flux.fromIterable(results.get(0).valueStream().toList()))
                .map(SurrealSecurityUserRepository::toUser);
    }

    private static SecurityUser toUser(JsonNode row) {
        return new SecurityUser(
                new UserId(UUID.fromString(SurrealRecordId.idPart(row.path("id").asString()))),
                new TenantId(row.path("tenantId").asString()),
                new Email(row.path("email").asString()),
                row.path("name").asString(""),
                Instant.parse(row.path("createdAt").asString()),
                Instant.parse(row.path("lastLoginAt").asString()));
    }

    private static ExternalIdentity toIdentity(JsonNode row) {
        return new ExternalIdentity(
                new UserId(UUID.fromString(row.path("userId").asString())),
                row.path("issuer").asString(),
                row.path("subject").asString(),
                row.path("provider").asString());
    }
}
