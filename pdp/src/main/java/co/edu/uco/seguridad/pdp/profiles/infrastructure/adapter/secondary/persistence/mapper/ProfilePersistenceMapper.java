package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.secondary.persistence.mapper;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.domain.Profile;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.secondary.persistence.entity.ProfileEntity;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScopeLevel;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

/** Fila a dominio. Los value objects validan aquí; el alcance se reconstruye desde level + ids. Espejo de RolePersistenceMapper. */
public final class ProfilePersistenceMapper {

    private ProfilePersistenceMapper() {
    }

    public static Profile toDomain(ProfileEntity entity) {
        RoleScope scope = switch (RoleScopeLevel.parse(entity.level())) {
            case GLOBAL -> RoleScope.global();
            case TENANT -> RoleScope.ofTenant(new TenantId(entity.tenantId()));
            case APPLICATION -> RoleScope.ofApplication(new TenantId(entity.tenantId()),
                    ApplicationId.of(entity.applicationId()));
        };

        Set<RoleId> roles = entity.roleIds().stream().map(RoleId::of).collect(Collectors.toSet());

        return new Profile(ProfileId.of(entity.id()), new ProfileName(entity.name()), scope, roles,
                Instant.parse(entity.registeredAt()));
    }
}
