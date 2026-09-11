package co.edu.uco.seguridad.pdp.identity.domain.model;

import co.edu.uco.seguridad.pdp.identity.domain.exception.InvalidEmailException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTests {

    @Test
    void lowercases_and_trims_a_well_formed_email() {
        assertThat(new Email("  David@UCO.edu.co ").value()).isEqualTo("david@uco.edu.co");
    }

    @Test
    void rejects_a_null_value() {
        assertThatThrownBy(() -> new Email(null)).isInstanceOf(InvalidEmailException.class);
    }

    @Test
    void rejects_a_value_without_an_at_sign() {
        assertThatThrownBy(() -> new Email("not-an-email")).isInstanceOf(InvalidEmailException.class);
    }
}
