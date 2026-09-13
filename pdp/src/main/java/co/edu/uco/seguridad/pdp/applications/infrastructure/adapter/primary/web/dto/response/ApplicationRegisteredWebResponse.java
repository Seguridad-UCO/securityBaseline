package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response;

import java.time.Instant;

/**
 * Contrato HTTP de salida exclusivo del registro (HU-012): igual que {@link ApplicationWebResponse}
 * más el secreto en texto plano, mostrado una sola vez. {@code GET /api/v1/applications} sigue
 * usando {@link ApplicationWebResponse}, sin este campo.
 */
public record ApplicationRegisteredWebResponse(String id, String tenantId, String name, String description,
        String baseUrl, String credential, Instant registeredAt) {
}
