package co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.dto.response;

import java.time.Instant;

/**
 * El contrato HTTP de salida, propiedad del adaptador web. Plano y primitivo a propósito: publicar
 * los value objects directamente haría que renombrar un campo del dominio rompiera la API en silencio.
 */
public record UserWebResponse(String id, String email, String name, String provider, String tenantId,
        Instant createdAt, Instant lastLoginAt) {
}
