package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw;

/** El profileId lo pone el controller desde la ruta; el roleId viene en el cuerpo. Movido desde {@code profiles} (HU-019). */
public record AddRoleToProfileRawRequest(String profileId, String roleId) {
}
