package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * Nivel uno de la estrategia de entrada en dos niveles: {@code applicationId} viene del path (lo
 * completa el controller, HU-013), {@code secret} del cuerpo. Todo {@code String}, sin validar.
 */
public record ValidateApplicationCredentialRawRequest(String applicationName, String secret) {
}
