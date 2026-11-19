package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * Strings desnudos, sin anotaciones. El inquilino no viaja aquí: sale del principal. Movido desde {@code profiles} (HU-019).
 */
public record DefineProfileRawRequest(String name, String scope, String applicationId) {
}
