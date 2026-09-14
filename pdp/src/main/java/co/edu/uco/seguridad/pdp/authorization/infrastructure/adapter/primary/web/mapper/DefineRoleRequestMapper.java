package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.authorization.domain.message.AuthorizationMessages;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.DefineRoleRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.DefineRoleRequest;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScopeLevel;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;

/**
 * raw a {@code DefineRoleRequest} (HU-016): mismo cuerpo que tenía en {@code roles} — scope solo
 * {@code TENANT} o {@code APPLICATION} por este canal ({@code GLOBAL} se rechaza, HU-004),
 * {@code APPLICATION} exige {@code applicationId} y {@code TENANT} lo prohíbe. Los dos mensajes de
 * contrato salen de {@link AuthorizationMessages}, no de {@code RolesMessages}: el mapper vive en
 * este módulo desde que el endpoint se movió (PLAN-HU-016.md §7).
 */
public final class DefineRoleRequestMapper {

    private DefineRoleRequestMapper() {
    }

    public static DefineRoleRequest toRequest(DefineRoleRawRequest raw, TenantId tenantId) {
        RoleName name = RequestFieldParser.parse("name", raw.name(), RoleName::new);
        RoleScopeLevel level = RequestFieldParser.parse("scope", raw.scope(), RoleScopeLevel::parse);

        if (level == RoleScopeLevel.GLOBAL) {
            throw new MalformedRequestFieldException("scope", AuthorizationMessages.globalScopeNotAdministrableYet());
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
                    AuthorizationMessages.applicationIdNotApplicableForTenantScope());
        }
        return RoleScope.ofTenant(tenantId);
    }
}
