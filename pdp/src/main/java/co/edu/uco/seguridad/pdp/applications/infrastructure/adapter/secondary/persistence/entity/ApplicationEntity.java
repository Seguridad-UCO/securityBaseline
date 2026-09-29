package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.secondary.persistence.entity;

/**
 * La forma de la fila en SurrealDB, no la del dominio: todo {@code String}, sin invariantes.
 *
 * <p>Existe para que el adaptador no construya la entidad de dominio directamente desde el JSON.
 * Separar «leer la fila» de «construir el agregado» deja el mapeo en un solo sitio y hace que este
 * slice se lea igual que los demás.
 */
public record ApplicationEntity(String id, String tenantId, String name, String description, String baseUrl,
                                String credentialHash, String registeredAt) {
}
