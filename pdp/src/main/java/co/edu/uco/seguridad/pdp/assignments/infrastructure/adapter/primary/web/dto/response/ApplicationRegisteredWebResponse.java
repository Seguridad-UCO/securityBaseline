package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response;

import java.time.Instant;

/**
 * Misma forma que {@code ApplicationRegisteredWebResponse} de {@code applications} — dueño distinto
 * (este endpoint vive en {@code assignments} desde HU-015, ver PLAN-HU-015.md §0).
 */
public record ApplicationRegisteredWebResponse(String id, String tenantId, String name, String description,
        String baseUrl, String credential, Instant registeredAt) {
}
