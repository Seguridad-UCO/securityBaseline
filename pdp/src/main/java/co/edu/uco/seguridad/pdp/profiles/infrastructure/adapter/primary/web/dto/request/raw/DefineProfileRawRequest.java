package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.request.raw;

/** Strings desnudos, sin anotaciones. El inquilino no viaja aquí: sale del principal. */
public record DefineProfileRawRequest(String name, String scope, String applicationId) {
}
