package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.DefineRoleRequest;
import co.edu.uco.seguridad.pdp.roles.domain.message.RolesMessages;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScopeLevel;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.request.raw.DefineRoleRawRequest;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;

/**
 * raw a DefineRoleRequest con RequestFieldParser. Barreras C1 y C2 del plan: scope solo TENANT o
 * APPLICATION por este canal (GLOBAL se rechaza hasta HU-009, con RolesMessages.globalScopeNotAdministrableYet());
 * APPLICATION exige applicationId y TENANT lo prohíbe. El inquilino llega ya resuelto del principal.
 */
public final class DefineRoleRequestMapper {

    private DefineRoleRequestMapper() {
    }

    public static DefineRoleRequest toRequest(DefineRoleRawRequest raw, TenantId tenantId) {
        RoleName name = RequestFieldParser.parse("name", raw.name(), RoleName::new);
        RoleScopeLevel level = RequestFieldParser.parse("scope", raw.scope(), RoleScopeLevel::parse);

        if (level == RoleScopeLevel.GLOBAL) {
            throw new MalformedRequestFieldException("scope", RolesMessages.globalScopeNotAdministrableYet());
        }

        RoleScope scope = level == RoleScopeLevel.APPLICATION
                ? RoleScope.ofApplication(tenantId,
                        RequestFieldParser.parse("applicationId", raw.applicationId(), ApplicationId::of))
                : requireNoApplicationId(raw, tenantId);

        return new DefineRoleRequest(name, scope);
    }

    private static RoleScope requireNoApplicationId(DefineRoleRawRequest raw, TenantId tenantId) {
        if (RequestFieldParser.optional(raw.applicationId()).isPresent()) {
            throw new MalformedRequestFieldException("applicationId",
                    RolesMessages.applicationIdNotApplicableForTenantScope());
        }
        return RoleScope.ofTenant(tenantId);
    }
}
