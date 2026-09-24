package co.edu.uco.seguridad.pdp.roles.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;

public record UpdateRoleRequest(TenantId tenantId, RoleId roleId, RoleName name) { }
