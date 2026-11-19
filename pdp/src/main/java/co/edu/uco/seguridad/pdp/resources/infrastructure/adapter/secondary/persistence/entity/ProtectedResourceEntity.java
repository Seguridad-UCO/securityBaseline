package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.secondary.persistence.entity;

import java.time.Instant;

/**
 * Representación de persistencia de una fila de catálogo: plana, tipada primitivamente e inmutable.
 */
public record ProtectedResourceEntity(String id, String applicationId, String tenantId, String path, String method,
                                      Instant registeredAt) {
}
