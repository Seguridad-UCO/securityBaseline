package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response;

import java.time.Instant;

/**
 * El contrato HTTP de salida, propiedad del adaptador web. Plano y primitivo a propósito: publicar
 * los value objects directamente haría que renombrar un campo del dominio rompiera la API en silencio.
 */
public record ApplicationWebResponse(String id, String tenantId, String name, String description, String baseUrl,
        Instant registeredAt) {
}
