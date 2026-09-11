package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw;

/** Strings desnudos, sin anotaciones. El tenant y el sujeto no viajan aqui: salen del principal. */
public record AuthorizeRawRequest(String applicationId, String resourcePath, String action) {
}
