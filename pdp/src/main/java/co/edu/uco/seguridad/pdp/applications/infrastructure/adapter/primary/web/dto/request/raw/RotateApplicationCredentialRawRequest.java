package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * Nivel uno de la estrategia de entrada en dos niveles: solo el {@code applicationId} del path — sin
 * cuerpo (HU-014). El tenant sale del principal, nunca de aquí.
 */
public record RotateApplicationCredentialRawRequest(String applicationId) {
}
