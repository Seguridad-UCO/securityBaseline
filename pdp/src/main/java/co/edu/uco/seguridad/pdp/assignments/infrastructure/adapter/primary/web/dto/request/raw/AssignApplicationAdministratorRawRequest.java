package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw;

/** El applicationId lo pone el controller desde la ruta; userId viene en el cuerpo. */
public record AssignApplicationAdministratorRawRequest(String applicationId, String userId) {
}
