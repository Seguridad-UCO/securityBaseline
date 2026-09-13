package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.secondary.persistence.mapper;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.domain.Profile;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.secondary.persistence.entity.ProfileEntity;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Espejo de RolePersistenceMapperTests. */
class ProfilePersistenceMapperTests {

    private static final String ID = UUID.randomUUID().toString();
    private static final String TENANT = "universidad-uco";
    private static final String APPLICATION = UUID.randomUUID().toString();
    private static final String REGISTERED_AT = "2026-09-12T00:00:00Z";

    @Test
    void reconstructs_a_global_profile() {
        ProfileEntity entity = new ProfileEntity(ID, "Coordinador académico", "GLOBAL", null, null, List.of(), REGISTERED_AT);

        Profile profile = ProfilePersistenceMapper.toDomain(entity);

        assertThat(profile.scope()).isEqualTo(RoleScope.global());
    }

    @Test
    void reconstructs_a_tenant_scoped_profile() {
        ProfileEntity entity = new ProfileEntity(ID, "Coordinador académico", "TENANT", TENANT, null, List.of(), REGISTERED_AT);

        Profile profile = ProfilePersistenceMapper.toDomain(entity);

        assertThat(profile.scope()).isEqualTo(RoleScope.ofTenant(new TenantId(TENANT)));
    }

    @Test
    void reconstructs_an_application_scoped_profile() {
        ProfileEntity entity = new ProfileEntity(ID, "Coordinador académico", "APPLICATION", TENANT, APPLICATION,
                List.of(), REGISTERED_AT);

        Profile profile = ProfilePersistenceMapper.toDomain(entity);

        assertThat(profile.scope()).isEqualTo(RoleScope.ofApplication(new TenantId(TENANT), ApplicationId.of(APPLICATION)));
    }

    @Test
    void an_empty_role_list_becomes_an_empty_set() {
        ProfileEntity entity = new ProfileEntity(ID, "Coordinador académico", "GLOBAL", null, null, List.of(), REGISTERED_AT);

        Profile profile = ProfilePersistenceMapper.toDomain(entity);

        assertThat(profile.roles()).isEmpty();
    }
}
