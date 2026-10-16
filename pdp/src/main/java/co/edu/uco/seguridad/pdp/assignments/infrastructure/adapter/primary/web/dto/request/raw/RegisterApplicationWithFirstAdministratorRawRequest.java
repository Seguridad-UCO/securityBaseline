package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * Mismos campos que {@code RegisterApplicationRawRequest} de {@code applications} — dueño distinto
 * (este endpoint vive en {@code assignments}, ver PLAN-HU-015.md §0), no se reutiliza el DTO de
 * infraestructura de otro módulo. Sin {@code tenantId} ni {@code userId}: ambos salen del principal.
 */
public record RegisterApplicationWithFirstAdministratorRawRequest(String name, String description, String baseUrl,
                                                                   java.util.Set<String> enabledEnvironments) {
    public RegisterApplicationWithFirstAdministratorRawRequest(String name, String description, String baseUrl) {
        this(name, description, baseUrl, java.util.Set.of());
    }
}
