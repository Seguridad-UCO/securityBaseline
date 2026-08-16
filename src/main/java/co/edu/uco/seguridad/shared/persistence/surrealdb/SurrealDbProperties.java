package co.edu.uco.seguridad.shared.persistence.surrealdb;

import co.edu.uco.seguridad.crosscutting.messages.RequiredArgumentMessages;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

/**
 * Conexión al único almacén real del contenedor PDP (ADR-019). Namespace/database compartido entre
 * módulos: Modulith ya traza esa frontera a nivel de código.
 *
 * @param url       base HTTP de la instancia SurrealDB (p. ej. {@code http://localhost:8000})
 * @param namespace namespace SurrealDB del contenedor PDP
 * @param database  database SurrealDB del contenedor PDP
 * @param username  usuario con el que la aplicación se autentica
 * @param password  contraseña — real en `qa`/`prod`, ver `infra/README.md` (`pdp-datasource-password`)
 */
@ConfigurationProperties(prefix = "pdp.persistence.surrealdb")
public record SurrealDbProperties(String url, String namespace, String database, String username, String password) {

    public SurrealDbProperties {
        Objects.requireNonNull(url, RequiredArgumentMessages.SURREALDB_URL);
        Objects.requireNonNull(namespace, RequiredArgumentMessages.SURREALDB_NAMESPACE);
        Objects.requireNonNull(database, RequiredArgumentMessages.SURREALDB_DATABASE);
        Objects.requireNonNull(username, RequiredArgumentMessages.SURREALDB_USERNAME);
        Objects.requireNonNull(password, RequiredArgumentMessages.SURREALDB_PASSWORD);
    }
}
