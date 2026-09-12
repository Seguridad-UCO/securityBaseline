package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignRoleRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.AssignRoleRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;

/** raw a AssignRoleRequest con RequestFieldParser (RoleId::of, UserId::of, ApplicationId::of). */
public final class AssignRoleRequestMapper {

    private AssignRoleRequestMapper() {
    }

    public static AssignRoleRequest toRequest(AssignRoleRawRequest raw, TenantId tenantId) {
        RoleId roleId = RequestFieldParser.parse("roleId", raw.roleId(), RoleId::of);
        UserId userId = RequestFieldParser.parse("userId", raw.userId(), UserId::of);
        ApplicationId applicationId = RequestFieldParser.parse("applicationId", raw.applicationId(), ApplicationId::of);
        return new AssignRoleRequest(tenantId, userId, applicationId, roleId);
    }
}
