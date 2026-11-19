package co.edu.uco.seguridad.pdp.profiles.domain.model;

import co.edu.uco.seguridad.pdp.profiles.domain.exception.InvalidProfileNameException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileNameTests {

    @Test
    void trims_and_keeps_a_well_formed_name() {
        assertThat(new ProfileName("  Coordinador académico  ").value()).isEqualTo("Coordinador académico");
    }

    @Test
    void rejects_a_null_value() {
        assertThatThrownBy(() -> new ProfileName(null)).isInstanceOf(InvalidProfileNameException.class);
    }

    @Test
    void rejects_a_blank_value() {
        assertThatThrownBy(() -> new ProfileName("   ")).isInstanceOf(InvalidProfileNameException.class);
    }

    @Test
    void rejects_a_name_shorter_than_three_characters() {
        assertThatThrownBy(() -> new ProfileName("ab")).isInstanceOf(InvalidProfileNameException.class);
    }

    @Test
    void accepts_a_name_with_exactly_three_characters() {
        assertThat(new ProfileName("abc").value()).isEqualTo("abc");
    }

    @Test
    void accepts_a_name_with_exactly_sixty_characters() {
        String sixty = "a".repeat(60);
        assertThat(new ProfileName(sixty).value()).isEqualTo(sixty);
    }

    @Test
    void rejects_a_name_longer_than_sixty_characters() {
        assertThatThrownBy(() -> new ProfileName("a".repeat(61))).isInstanceOf(InvalidProfileNameException.class);
    }
}
