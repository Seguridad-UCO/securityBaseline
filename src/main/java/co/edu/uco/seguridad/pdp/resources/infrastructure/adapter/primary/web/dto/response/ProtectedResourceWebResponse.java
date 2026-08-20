package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.response;

import java.time.Instant;

/**
 * El contrato HTTP de salida, propiedad del adaptador web. Plano y primitivo a propósito: publicar
 * los value objects directamente haría que renombrar un campo del dominio rompiera la API en silencio.
 */
public record ProtectedResourceWebResponse(String id, String applicationId, String tenantId, String path,
        String method, Instant registeredAt) {
}
