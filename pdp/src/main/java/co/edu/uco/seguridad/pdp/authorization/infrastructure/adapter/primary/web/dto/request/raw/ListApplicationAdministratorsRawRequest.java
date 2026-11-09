package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * El applicationId sale de la ruta, sin cuerpo ni query params (HU-020).
 */
public record ListApplicationAdministratorsRawRequest(String applicationId) {
}
