package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * Espejo de {@code SolicitudAcceso v1} ({@code contracts/pep-pdp/v1/request.schema.json}). Strings
 * desnudos, sin anotaciones — la validación vive en {@code AccessDecisionRawRequestMapper}.
 */
public record AccessDecisionRawRequest(String version, String requestId, String correlationId,
        String timestamp, RawApplication application, RawResource resource, RawContext context) {

    public record RawApplication(String id, String environment) {
    }

    public record RawResource(String path, String action) {
    }

    public record RawContext(String method, String channel) {
    }
}
