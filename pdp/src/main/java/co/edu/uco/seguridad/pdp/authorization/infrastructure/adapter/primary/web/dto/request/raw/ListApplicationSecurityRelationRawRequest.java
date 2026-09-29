package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw;

/** HTTP input for an entity relationship page, still parsed at the primary adapter boundary. */
public record ListApplicationSecurityRelationRawRequest(String applicationId, String entityId, String page, String size, String offset, String limit) { }
