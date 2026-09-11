package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * Nivel uno de la estrategia de entrada en dos niveles: el JSON exactamente como llegó, todo
 * {@code String} y sin validar a propósito. {@code applicationId} llega de la ruta, no del cuerpo,
 * pero viaja junto al resto porque el interactor solo recibe un valor de entrada.
 */
public record RegisterProtectedResourceRawRequest(String applicationId, String path, String method) {
}
