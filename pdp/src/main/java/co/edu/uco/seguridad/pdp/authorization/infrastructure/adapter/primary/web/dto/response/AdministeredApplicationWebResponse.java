package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response;

import java.time.Instant;

/**
 * Contrato HTTP de la rotación de credencial gateada (HU-015). Misma forma que
 * {@code ApplicationRegisteredWebResponse} de {@code applications} — dueño distinto (este endpoint
 * vive en {@code authorization}, ver PLAN-HU-015.md §0), no se reutiliza la clase de infraestructura
 * de otro módulo.
 */
public record AdministeredApplicationWebResponse(String id, String tenantId, String name, String description,
                                                 String baseUrl, String credential, Instant registeredAt,
                                                 java.util.Set<String> enabledEnvironments) {
    public AdministeredApplicationWebResponse(String id, String tenantId, String name, String description,
                                              String baseUrl, String credential, Instant registeredAt) {
        this(id, tenantId, name, description, baseUrl, credential, registeredAt, java.util.Set.of());
    }
}
