package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response;
/** Administrative projection: ids remain usable internally, but operators never have to resolve them. */
public record ApplicationSecurityProfileAssignmentWebResponse(String id, ApplicationSecurityUserWebResponse user,
        ApplicationSecurityProfileWebResponse profile, String validFrom, String validUntil) {}
