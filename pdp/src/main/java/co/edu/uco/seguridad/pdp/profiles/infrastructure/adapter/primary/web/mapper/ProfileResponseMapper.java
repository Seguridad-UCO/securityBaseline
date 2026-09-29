package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.response.ProfileWebResponse;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;

/**
 * ProfileResponse (value objects) a ProfileWebResponse (plana): ningún VO ni enum de dominio cruza al cliente.
 */
public final class ProfileResponseMapper {

    private ProfileResponseMapper() {
    }

    public static ProfileWebResponse toResponse(ProfileResponse response) {
        RoleScope scope = response.scope();
        return new ProfileWebResponse(
                response.id().value().toString(),
                response.name().value(),
                scope.level().name(),
                scope.tenantId().map(tenantId -> tenantId.value()).orElse(null),
                scope.applicationId().map(applicationId -> applicationId.value().toString()).orElse(null),
                response.roles().stream().map(RoleId::value).map(Object::toString).toList(),
                response.registeredAt().toString());
    }
}
