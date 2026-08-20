package co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.secondary.persistence.schema;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.Map;
import java.util.Objects;

/** Define las tablas {@code security_user}/{@code external_identity} y sus índices únicos (ADR-0004). */
public final class SurrealIdentitySchemaInitializer implements ApplicationRunner {

    private final SurrealDbClient client;

    public SurrealIdentitySchemaInitializer(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, RequiredArgumentMessages.SURREALDB_CLIENT);
    }

    @Override
    public void run(ApplicationArguments args) {
        client.ensureNamespaceAndDatabase()
                .then(client.execute("""
                        DEFINE TABLE IF NOT EXISTS %1$s SCHEMALESS;
                        DEFINE TABLE IF NOT EXISTS %2$s SCHEMALESS;
                        DEFINE INDEX IF NOT EXISTS external_identity_issuer_subject ON %2$s COLUMNS issuer, subject UNIQUE;
                        DEFINE INDEX IF NOT EXISTS security_user_email ON %1$s COLUMNS email UNIQUE;
                        """.formatted(IdentitySchema.USER_TABLE, IdentitySchema.IDENTITY_TABLE), Map.of()))
                .then()
                .block();
    }
}
