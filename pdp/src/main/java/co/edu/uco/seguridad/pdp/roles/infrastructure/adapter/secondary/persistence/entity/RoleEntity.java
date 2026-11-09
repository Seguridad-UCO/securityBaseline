package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.secondary.persistence.entity;

import java.util.List;

/**
 * La forma de la fila, no del dominio: Strings, y ausencias como null (tenantId si global; applicationId salvo APPLICATION).
 */
public record RoleEntity(String id, String name, String level, String tenantId, String applicationId,
                         List<String> resourceIds, String registeredAt) {
}
