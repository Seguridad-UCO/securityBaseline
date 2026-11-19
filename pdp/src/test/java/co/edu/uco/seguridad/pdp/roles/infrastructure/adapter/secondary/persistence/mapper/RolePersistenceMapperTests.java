package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.secondary.persistence.mapper;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.domain.Role;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.secondary.persistence.entity.RoleEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RolePersistenceMapperTests {

    private static final String ID = UUID.randomUUID().toString();
    private static final String TENANT = "universidad-uco";
    private static final String APPLICATION = UUID.randomUUID().toString();
    private static final String REGISTERED_AT = "2026-09-11T00:00:00Z";

    @Test
    void reconstructs_a_global_role() {
        RoleEntity entity = new RoleEntity(ID, "Docente", "GLOBAL", null, null, List.of(), REGISTERED_AT);

        Role role = RolePersistenceMapper.toDomain(entity);

        assertThat(role.scope()).isEqualTo(RoleScope.global());
    }

    @Test
    void reconstructs_a_tenant_scoped_role() {
        RoleEntity entity = new RoleEntity(ID, "Docente", "TENANT", TENANT, null, List.of(), REGISTERED_AT);

        Role role = RolePersistenceMapper.toDomain(entity);

        assertThat(role.scope()).isEqualTo(RoleScope.ofTenant(new TenantId(TENANT)));
    }

    @Test
    void reconstructs_an_application_scoped_role() {
        RoleEntity entity = new RoleEntity(ID, "Docente", "APPLICATION", TENANT, APPLICATION, List.of(), REGISTERED_AT);

        Role role = RolePersistenceMapper.toDomain(entity);

        assertThat(role.scope()).isEqualTo(RoleScope.ofApplication(new TenantId(TENANT), ApplicationId.of(APPLICATION)));
    }

    @Test
    void an_empty_resource_list_becomes_an_empty_set() {
        RoleEntity entity = new RoleEntity(ID, "Docente", "GLOBAL", null, null, List.of(), REGISTERED_AT);

        Role role = RolePersistenceMapper.toDomain(entity);

        assertThat(role.resources()).isEmpty();
    }
}
