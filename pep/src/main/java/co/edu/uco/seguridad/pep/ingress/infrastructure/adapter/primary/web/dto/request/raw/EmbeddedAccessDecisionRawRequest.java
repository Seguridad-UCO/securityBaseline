package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * Solicitud que una aplicación protegida envía al PEP embebido. La identidad del usuario no viaja
 * en el cuerpo: siempre se conserva en el encabezado {@code Authorization} original.
 */
public record EmbeddedAccessDecisionRawRequest(String applicationName, String environment, String path, String method) {
}
