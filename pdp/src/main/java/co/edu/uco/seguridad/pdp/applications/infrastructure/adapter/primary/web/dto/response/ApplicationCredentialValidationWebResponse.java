package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response;

/**
 * Contrato HTTP de salida del canal interno de validación (HU-013). Desnudo a propósito, sin
 * {@code ApiResponse} — mismo criterio D6 de HU-003: es un canal máquina-a-máquina.
 */
public record ApplicationCredentialValidationWebResponse(String tenantId) {
}
