package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * DTO crudo (HU-017): misma forma que {@code resources...raw.RegisterProtectedResourceRawRequest} —
 * vive aquí porque el endpoint que lo recibe se mueve a {@code authorization} (ver PLAN-HU-017.md §6).
 * {@code applicationId} llega de la ruta, no del cuerpo, pero viaja junto al resto porque el
 * interactor solo recibe un valor de entrada.
 */
public record RegisterProtectedResourceRawRequest(String applicationId, String path, String method) {
}
