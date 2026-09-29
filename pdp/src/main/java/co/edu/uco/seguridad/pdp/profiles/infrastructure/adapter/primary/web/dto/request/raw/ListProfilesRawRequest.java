package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * Parámetros de consulta tal como llegan por HTTP: todos String y todos opcionales.
 */
public record ListProfilesRawRequest(String page, String size, String offset, String limit) {
}
