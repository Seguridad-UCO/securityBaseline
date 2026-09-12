package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.secondary.persistence.mapper;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.domain.Role;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScopeLevel;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.secondary.persistence.entity.RoleEntity;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

/** Fila a dominio. Los value objects validan aquí; el alcance se reconstruye desde level + ids. */
public final class RolePersistenceMapper {

    private RolePersistenceMapper() {
    }

    public static Role toDomain(RoleEntity entity) {
        RoleScope scope = switch (RoleScopeLevel.parse(entity.level())) {
            case GLOBAL -> RoleScope.global();
            case TENANT -> RoleScope.ofTenant(new TenantId(entity.tenantId()));
            case APPLICATION -> RoleScope.ofApplication(new TenantId(entity.tenantId()),
                    ApplicationId.of(entity.applicationId()));
        };

        Set<ResourceId> resources = entity.resourceIds().stream().map(ResourceId::of).collect(Collectors.toSet());

        return new Role(RoleId.of(entity.id()), new RoleName(entity.name()), scope, resources,
                Instant.parse(entity.registeredAt()));
    }
}
