package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.DefineProfileRequest;
import co.edu.uco.seguridad.pdp.profiles.domain.message.ProfilesMessages;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScopeLevel;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.request.raw.DefineProfileRawRequest;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;

/**
 * raw a DefineProfileRequest con RequestFieldParser. Mismas barreras que DefineRoleRequestMapper:
 * scope solo TENANT o APPLICATION por este canal (GLOBAL se rechaza hasta HU-009); APPLICATION
 * exige applicationId y TENANT lo prohíbe. El inquilino llega ya resuelto del principal.
 */
public final class DefineProfileRequestMapper {

    private DefineProfileRequestMapper() {
    }

    public static DefineProfileRequest toRequest(DefineProfileRawRequest raw, TenantId tenantId) {
        ProfileName name = RequestFieldParser.parse("name", raw.name(), ProfileName::new);
        RoleScopeLevel level = RequestFieldParser.parse("scope", raw.scope(), RoleScopeLevel::parse);

        if (level == RoleScopeLevel.GLOBAL) {
            throw new MalformedRequestFieldException("scope", ProfilesMessages.globalScopeNotAdministrableYet());
        }

        RoleScope scope = level == RoleScopeLevel.APPLICATION
                ? RoleScope.ofApplication(tenantId,
                        RequestFieldParser.parse("applicationId", raw.applicationId(), ApplicationId::of))
                : requireNoApplicationId(raw, tenantId);

        return new DefineProfileRequest(name, scope);
    }

    private static RoleScope requireNoApplicationId(DefineProfileRawRequest raw, TenantId tenantId) {
        if (RequestFieldParser.optional(raw.applicationId()).isPresent()) {
            throw new MalformedRequestFieldException("applicationId",
                    ProfilesMessages.applicationIdNotApplicableForTenantScope());
        }
        return RoleScope.ofTenant(tenantId);
    }
}
