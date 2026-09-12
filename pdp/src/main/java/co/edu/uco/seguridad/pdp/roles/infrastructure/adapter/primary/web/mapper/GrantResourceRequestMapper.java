package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.GrantResourceRequest;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.request.raw.GrantResourceRawRequest;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;

/** raw a GrantResourceRequest: roleId y resourceId con RequestFieldParser (RoleId::of, ResourceId::of). */
public final class GrantResourceRequestMapper {

    private GrantResourceRequestMapper() {
    }

    public static GrantResourceRequest toRequest(GrantResourceRawRequest raw, TenantId tenantId) {
        RoleId roleId = RequestFieldParser.parse("roleId", raw.roleId(), RoleId::of);
        ResourceId resourceId = RequestFieldParser.parse("resourceId", raw.resourceId(), ResourceId::of);
        return new GrantResourceRequest(tenantId, roleId, resourceId);
    }
}
