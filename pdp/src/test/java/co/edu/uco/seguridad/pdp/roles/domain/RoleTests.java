package co.edu.uco.seguridad.pdp.roles.domain;

import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTests {

    private static final RoleId ID = new RoleId(UUID.randomUUID());
    private static final RoleName NAME = new RoleName("Docente");
    private static final RoleScope SCOPE = RoleScope.ofTenant(new TenantId("universidad-uco"));
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-11T00:00:00Z");

    @Test
    void define_starts_with_no_resources() {
        Role role = Role.define(ID, NAME, SCOPE, REGISTERED_AT);

        assertThat(role.resources()).isEmpty();
        assertThat(role.id()).isEqualTo(ID);
        assertThat(role.name()).isEqualTo(NAME);
        assertThat(role.scope()).isEqualTo(SCOPE);
        assertThat(role.registeredAt()).isEqualTo(REGISTERED_AT);
    }

    @Test
    void with_resource_adds_a_resource_without_mutating_the_original() {
        Role role = Role.define(ID, NAME, SCOPE, REGISTERED_AT);
        ResourceId resource = new ResourceId(UUID.randomUUID());

        Role withResource = role.withResource(resource);

        assertThat(withResource.resources()).containsExactly(resource);
        assertThat(role.resources()).isEmpty();
    }

    @Test
    void granting_the_same_resource_twice_does_not_duplicate_it() {
        Role role = Role.define(ID, NAME, SCOPE, REGISTERED_AT);
        ResourceId resource = new ResourceId(UUID.randomUUID());

        Role granted = role.withResource(resource).withResource(resource);

        assertThat(granted.resources()).hasSize(1);
    }
}
