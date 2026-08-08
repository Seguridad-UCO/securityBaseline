package co.edu.uco.seguridad.shared.persistence.surrealdb;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

/**
 * Conexión al único almacén real del contenedor PDP (ADR-0004). Un namespace/database compartido
 * entre módulos porque hoy son un solo proceso desplegable; separarlos por módulo sería una frontera
 * que Spring Modulith ya traza a nivel de código y que la base de datos no necesita repetir.
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
        Objects.requireNonNull(url, "se requiere la URL de SurrealDB (pdp.persistence.surrealdb.url)");
        Objects.requireNonNull(namespace, "se requiere el namespace de SurrealDB");
        Objects.requireNonNull(database, "se requiere la database de SurrealDB");
        Objects.requireNonNull(username, "se requiere el usuario de SurrealDB");
        Objects.requireNonNull(password, "se requiere la contraseña de SurrealDB");
    }
}
