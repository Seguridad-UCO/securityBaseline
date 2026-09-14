package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.RoleAdministrationWebResponse;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;

/**
 * {@code RoleResponse} (value objects) a {@code RoleAdministrationWebResponse} (plana) — idéntico a
 * {@code roles...RoleResponseMapper}: ningún VO ni enum de dominio cruza al cliente.
 */
public final class RoleAdministrationResponseMapper {

    private RoleAdministrationResponseMapper() {
    }

    public static RoleAdministrationWebResponse toResponse(RoleResponse response) {
        RoleScope scope = response.scope();
        return new RoleAdministrationWebResponse(
                response.id().value().toString(),
                response.name().value(),
                scope.level().name(),
                scope.tenantId().map(tenantId -> tenantId.value()).orElse(null),
                scope.applicationId().map(applicationId -> applicationId.value().toString()).orElse(null),
                response.resources().stream().map(ResourceId::value).map(Object::toString).toList(),
                response.registeredAt().toString());
    }
}
