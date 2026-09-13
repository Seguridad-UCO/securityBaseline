package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.request.raw;

/** El profileId lo pone el controller desde la ruta; el roleId viene en el cuerpo. */
public record AddRoleToProfileRawRequest(String profileId, String roleId) {
}
