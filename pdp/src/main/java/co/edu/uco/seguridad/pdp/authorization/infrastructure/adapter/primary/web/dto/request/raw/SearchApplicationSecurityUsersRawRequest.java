package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw;
public record SearchApplicationSecurityUsersRawRequest(String applicationId,String query,String page,String size,String offset,String limit){}
