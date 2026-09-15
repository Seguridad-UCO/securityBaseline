package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw;

/** Ambos campos salen de la ruta, sin cuerpo (HU-020). */
public record RemoveApplicationAdministratorRawRequest(String applicationId, String userId) {
}
