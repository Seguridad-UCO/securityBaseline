package co.edu.uco.seguridad.pdp.authorization.infrastructure.serviceidentity.web;

import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceAccessGrant;

import java.time.Instant;

record ServiceAccessGrantWebResponse(String id, String serviceIdentityId, String targetApplicationId, String resourceId,
                                     String technicalRoleId, String technicalRole, String status, Instant requestedAt,
                                     Instant decidedAt) {
    static ServiceAccessGrantWebResponse from(ServiceAccessGrant grant) {
        return new ServiceAccessGrantWebResponse(grant.id().toString(), grant.serviceIdentityId().value().toString(),
                grant.targetApplicationId().value().toString(), grant.resourceId() == null ? null : grant.resourceId().value().toString(), grant.technicalRoleId() == null ? null : grant.technicalRoleId().toString(), grant.technicalRole(),
                grant.status().name(), grant.requestedAt(), grant.decidedAt());
    }
}
