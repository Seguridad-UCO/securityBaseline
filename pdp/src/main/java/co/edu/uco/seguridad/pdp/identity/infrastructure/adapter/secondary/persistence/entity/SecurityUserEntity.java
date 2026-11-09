package co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.secondary.persistence.entity;

/**
 * La forma de la fila del usuario en SurrealDB: todo {@code String}, sin invariantes.
 * El agregado se reconstruye en {@code SecurityUserPersistenceMapper}.
 */
public record SecurityUserEntity(String id, String tenantId, String email, String name, String createdAt,
                                 String lastLoginAt) {
}
