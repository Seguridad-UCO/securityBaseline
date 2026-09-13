package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw;

/** El profileId lo pone el controller desde la ruta; userId y applicationId vienen en el cuerpo. */
public record AssignProfileRawRequest(String profileId, String userId, String applicationId) {
}
