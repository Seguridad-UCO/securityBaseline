package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.response;

import java.time.Instant;

/**
 * Contrato HTTP de salida de HU-010, propiedad del adaptador web. Plano a propósito, sin anidar el
 * DTO web de {@code applications} (esta clase vive en la capa de infraestructura de {@code
 * resources}, y la de {@code applications} no está publicada fuera de su propio módulo — solo su
 * capa {@code application} lo está).
 */
public record ApplicationWithInitialResourceWebResponse(String applicationId, String tenantId,
        String applicationName, String description, String baseUrl, String credential,
        Instant applicationRegisteredAt, String resourceId, String resourcePath, String resourceMethod,
        Instant resourceRegisteredAt) {
}
