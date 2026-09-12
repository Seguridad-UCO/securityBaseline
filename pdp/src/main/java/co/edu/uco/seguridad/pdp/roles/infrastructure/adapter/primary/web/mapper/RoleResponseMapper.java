package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.response.RoleWebResponse;

/** RoleResponse (value objects) a RoleWebResponse (plana): ningún VO ni enum de dominio cruza al cliente. */
public final class RoleResponseMapper {

    private RoleResponseMapper() {
    }

    public static RoleWebResponse toResponse(RoleResponse response) {
        RoleScope scope = response.scope();
        return new RoleWebResponse(
                response.id().value().toString(),
                response.name().value(),
                scope.level().name(),
                scope.tenantId().map(tenantId -> tenantId.value()).orElse(null),
                scope.applicationId().map(applicationId -> applicationId.value().toString()).orElse(null),
                response.resources().stream().map(ResourceId::value).map(Object::toString).toList(),
                response.registeredAt().toString());
    }
}
