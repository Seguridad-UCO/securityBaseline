package co.edu.uco.seguridad.pdp.roles.domain.model;

import co.edu.uco.seguridad.pdp.roles.domain.exception.InvalidRoleNameException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleNameTests {

    @Test
    void trims_and_keeps_a_well_formed_name() {
        assertThat(new RoleName("  Docente  ").value()).isEqualTo("Docente");
    }

    @Test
    void rejects_a_null_value() {
        assertThatThrownBy(() -> new RoleName(null)).isInstanceOf(InvalidRoleNameException.class);
    }

    @Test
    void rejects_a_blank_value() {
        assertThatThrownBy(() -> new RoleName("   ")).isInstanceOf(InvalidRoleNameException.class);
    }

    @Test
    void rejects_a_name_shorter_than_three_characters() {
        assertThatThrownBy(() -> new RoleName("ab")).isInstanceOf(InvalidRoleNameException.class);
    }

    @Test
    void accepts_a_name_with_exactly_three_characters() {
        assertThat(new RoleName("abc").value()).isEqualTo("abc");
    }

    @Test
    void accepts_a_name_with_exactly_sixty_characters() {
        String sixty = "a".repeat(60);
        assertThat(new RoleName(sixty).value()).isEqualTo(sixty);
    }

    @Test
    void rejects_a_name_longer_than_sixty_characters() {
        assertThatThrownBy(() -> new RoleName("a".repeat(61))).isInstanceOf(InvalidRoleNameException.class);
    }
}
