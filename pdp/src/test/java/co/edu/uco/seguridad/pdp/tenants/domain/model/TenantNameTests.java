package co.edu.uco.seguridad.pdp.tenants.domain.model;

import co.edu.uco.seguridad.pdp.tenants.domain.exception.InvalidTenantNameException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantNameTests {

    @Test
    void trims_and_keeps_a_well_formed_name() {
        assertThat(new TenantName("  Universidad UCO  ").value()).isEqualTo("Universidad UCO");
    }

    @Test
    void rejects_a_null_value() {
        assertThatThrownBy(() -> new TenantName(null)).isInstanceOf(InvalidTenantNameException.class);
    }

    @Test
    void rejects_a_name_shorter_than_three_characters() {
        assertThatThrownBy(() -> new TenantName("ab")).isInstanceOf(InvalidTenantNameException.class);
    }
}
