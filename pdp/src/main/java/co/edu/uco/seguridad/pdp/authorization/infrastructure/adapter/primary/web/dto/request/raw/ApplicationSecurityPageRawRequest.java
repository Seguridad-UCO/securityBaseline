package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw;

/** Parámetros de paginación de las lecturas administrativas por aplicación, conservados crudos en HTTP. */
public record ApplicationSecurityPageRawRequest(String page, String size, String offset, String limit) {
}
