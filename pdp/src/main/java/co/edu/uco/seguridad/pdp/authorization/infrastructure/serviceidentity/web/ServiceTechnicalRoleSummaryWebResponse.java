package co.edu.uco.seguridad.pdp.authorization.infrastructure.serviceidentity.web;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceTechnicalRoleDefinition;
import java.time.Instant;
record ServiceTechnicalRoleSummaryWebResponse(String id, String applicationId, String code, String name, long resourceCount, Instant createdAt) {
    static ServiceTechnicalRoleSummaryWebResponse from(ServiceTechnicalRoleDefinition role, long resourceCount) { return new ServiceTechnicalRoleSummaryWebResponse(role.id().toString(),role.applicationId().value().toString(),role.code(),role.name(),resourceCount,role.createdAt()); }
}
