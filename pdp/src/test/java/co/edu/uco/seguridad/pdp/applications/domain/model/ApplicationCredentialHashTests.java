package co.edu.uco.seguridad.pdp.applications.domain.model;

import co.edu.uco.seguridad.pdp.applications.domain.exception.InvalidApplicationCredentialHashException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationCredentialHashTests {

    @Test
    void rejects_a_null_value() {
        assertThatThrownBy(() -> new ApplicationCredentialHash(null))
                .isInstanceOf(InvalidApplicationCredentialHashException.class);
    }

    @Test
    void rejects_a_blank_value() {
        assertThatThrownBy(() -> new ApplicationCredentialHash("   "))
                .isInstanceOf(InvalidApplicationCredentialHashException.class);
    }

    @Test
    void accepts_an_opaque_hash_as_is() {
        ApplicationCredentialHash hash = new ApplicationCredentialHash("$2a$10$abcdefghijklmnopqrstuv");

        assertThat(hash.value()).isEqualTo("$2a$10$abcdefghijklmnopqrstuv");
    }
}
