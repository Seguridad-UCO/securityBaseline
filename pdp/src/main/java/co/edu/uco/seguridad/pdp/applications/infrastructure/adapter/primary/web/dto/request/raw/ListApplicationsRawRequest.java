package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * Parámetros de consulta tal como llegan por HTTP: todos {@code String} y todos opcionales.
 * Ningún tipo rico cruza el borde; la conversión y el rechazo viven en el mapper.
 *
 * <p>Esqueleto de la SPEC de HU-001.
 */
public record ListApplicationsRawRequest(String name, String page, String size, String offset, String limit) {
}
