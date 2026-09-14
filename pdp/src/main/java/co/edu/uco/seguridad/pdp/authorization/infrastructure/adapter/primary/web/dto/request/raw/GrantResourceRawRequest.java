package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * DTO crudo: Strings desnudos, sin anotaciones. Misma forma que
 * {@code roles.infrastructure...raw.GrantResourceRawRequest} (HU-016) — ver la nota de
 * {@link DefineRoleRawRequest} sobre por qué vive aquí.
 */
public record GrantResourceRawRequest(String roleId, String resourceId) {
}
