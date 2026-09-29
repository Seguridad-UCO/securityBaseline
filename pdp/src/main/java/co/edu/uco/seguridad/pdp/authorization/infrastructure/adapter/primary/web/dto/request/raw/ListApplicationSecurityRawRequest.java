package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw;
/** Ruta y parámetros crudos comunes de una colección de seguridad por aplicación. */
public record ListApplicationSecurityRawRequest(String applicationId, String page, String size, String offset, String limit) { }
