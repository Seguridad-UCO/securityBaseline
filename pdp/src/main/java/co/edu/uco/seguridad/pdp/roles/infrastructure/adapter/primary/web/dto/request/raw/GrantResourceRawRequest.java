package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.request.raw;

/** El roleId lo pone el controller desde la ruta; el resourceId viene en el cuerpo. */
public record GrantResourceRawRequest(String roleId, String resourceId) {
}
