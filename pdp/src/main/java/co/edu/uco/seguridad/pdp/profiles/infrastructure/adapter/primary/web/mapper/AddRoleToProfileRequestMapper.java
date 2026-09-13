package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.AddRoleToProfileRequest;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.request.raw.AddRoleToProfileRawRequest;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;

/** raw a AddRoleToProfileRequest: profileId y roleId con RequestFieldParser (ProfileId::of, RoleId::of). */
public final class AddRoleToProfileRequestMapper {

    private AddRoleToProfileRequestMapper() {
    }

    public static AddRoleToProfileRequest toRequest(AddRoleToProfileRawRequest raw, TenantId tenantId) {
        ProfileId profileId = RequestFieldParser.parse("profileId", raw.profileId(), ProfileId::of);
        RoleId roleId = RequestFieldParser.parse("roleId", raw.roleId(), RoleId::of);
        return new AddRoleToProfileRequest(tenantId, profileId, roleId);
    }
}
