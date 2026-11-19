package co.edu.uco.seguridad.pdp.profiles.domain;

import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileTests {

    private static final ProfileId ID = new ProfileId(UUID.randomUUID());
    private static final RoleScope SCOPE = RoleScope.ofTenant(new TenantId("universidad-uco"));
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-12T00:00:00Z");

    // ProfileName es un esqueleto que aún lanza en su constructor compacto (pendiente: HU-011): se
    // construye dentro de cada @Test, nunca como campo estático, para que el rojo quede localizado
    // en el caso que corresponde y no tumbe la clase entera al cargarla.
    @Test
    void define_starts_with_no_roles() {
        ProfileName name = new ProfileName("Coordinador académico");
        Profile profile = Profile.define(ID, name, SCOPE, REGISTERED_AT);

        assertThat(profile.roles()).isEmpty();
        assertThat(profile.id()).isEqualTo(ID);
        assertThat(profile.name()).isEqualTo(name);
        assertThat(profile.scope()).isEqualTo(SCOPE);
        assertThat(profile.registeredAt()).isEqualTo(REGISTERED_AT);
    }

    @Test
    void with_role_adds_a_role_without_mutating_the_original() {
        Profile profile = Profile.define(ID, new ProfileName("Coordinador académico"), SCOPE, REGISTERED_AT);
        RoleId role = new RoleId(UUID.randomUUID());

        Profile withRole = profile.withRole(role);

        assertThat(withRole.roles()).containsExactly(role);
        assertThat(profile.roles()).isEmpty();
    }

    @Test
    void adding_the_same_role_twice_does_not_duplicate_it() {
        Profile profile = Profile.define(ID, new ProfileName("Coordinador académico"), SCOPE, REGISTERED_AT);
        RoleId role = new RoleId(UUID.randomUUID());

        Profile withRole = profile.withRole(role).withRole(role);

        assertThat(withRole.roles()).hasSize(1);
    }
}
