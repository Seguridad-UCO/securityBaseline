package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.secondary.persistence.entity;

import java.util.List;

/** La forma de la fila, no del dominio: Strings, y ausencias como null (tenantId si global; applicationId salvo APPLICATION). */
public record ProfileEntity(String id, String name, String level, String tenantId, String applicationId,
        List<String> roleIds, String registeredAt) {
}
