package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * El cuerpo JSON exactamente como llegó al {@code POST}. {@code applicationId} no está aquí: llega
 * de la ruta, no del cuerpo — el controlador los combina en {@link RegisterProtectedResourceRawRequest}
 * antes de pasarlos al interactor, que solo recibe un valor de entrada.
 */
public record RegisterProtectedResourceBodyRequest(String path, String method) {
}
