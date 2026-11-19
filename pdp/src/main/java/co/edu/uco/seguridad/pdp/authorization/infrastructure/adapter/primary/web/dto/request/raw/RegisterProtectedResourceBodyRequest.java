package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * El cuerpo JSON exactamente como llegó al {@code POST} (HU-017) — misma forma que
 * {@code resources...raw.RegisterProtectedResourceBodyRequest}. {@code applicationId} no está aquí:
 * llega de la ruta.
 */
public record RegisterProtectedResourceBodyRequest(String path, String method) {
}
