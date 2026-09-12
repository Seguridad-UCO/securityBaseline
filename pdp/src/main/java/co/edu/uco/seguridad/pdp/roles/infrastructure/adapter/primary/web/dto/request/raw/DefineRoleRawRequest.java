package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.request.raw;

/** Strings desnudos, sin anotaciones. El inquilino no viaja aquí: sale del principal. */
public record DefineRoleRawRequest(String name, String scope, String applicationId) {
}
