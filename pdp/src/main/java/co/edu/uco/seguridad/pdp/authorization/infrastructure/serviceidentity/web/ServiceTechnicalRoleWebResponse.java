package co.edu.uco.seguridad.pdp.authorization.infrastructure.serviceidentity.web;

import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceTechnicalRoleDefinition;
import java.time.Instant;
import java.util.Set;

record ServiceTechnicalRoleWebResponse(String id, String applicationId, String code, String name,
                                       Set<String> resourceIds, Instant createdAt) {
    static ServiceTechnicalRoleWebResponse from(ServiceTechnicalRoleDefinition role, Set<String> resourceIds) {
        return new ServiceTechnicalRoleWebResponse(role.id().toString(), role.applicationId().value().toString(), role.code(), role.name(), resourceIds, role.createdAt());
    }
}
