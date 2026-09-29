package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * El profileId lo pone el controller desde la ruta; el resto son parámetros de consulta opcionales.
 */
public record ListProfileAssignmentsRawRequest(String profileId, String page, String size, String offset,
                                               String limit) {
}
