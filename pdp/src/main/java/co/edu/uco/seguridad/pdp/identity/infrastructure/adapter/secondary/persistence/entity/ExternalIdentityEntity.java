package co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.secondary.persistence.entity;

/**
 * La forma de la fila de la identidad externa en SurrealDB: todo {@code String}, sin invariantes.
 */
public record ExternalIdentityEntity(String userId, String issuer, String subject, String provider) {
}
