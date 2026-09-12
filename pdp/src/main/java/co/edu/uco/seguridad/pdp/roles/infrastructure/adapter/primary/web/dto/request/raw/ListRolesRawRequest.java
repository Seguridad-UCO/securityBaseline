package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.request.raw;

/** Parámetros de consulta tal como llegan por HTTP: todos String y todos opcionales. */
public record ListRolesRawRequest(String page, String size, String offset, String limit) {
}
