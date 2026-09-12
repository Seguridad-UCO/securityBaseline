package co.edu.uco.seguridad.pdp.applications.infrastructure.properties;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code reservedNames} llega directo de {@code application.properties} — nunca se asume que
 * Spring siempre inyecta un conjunto no nulo cuando la clave {@code pdp.applications} está ausente.
 */
class ApplicationCatalogPropertiesTests {

    @Test
    void a_missing_reserved_names_list_becomes_empty_instead_of_a_null_set() {
        assertThat(new ApplicationCatalogProperties(null).reservedNames()).isEmpty();
    }

    @Test
    void keeps_the_configured_reserved_names() {
        assertThat(new ApplicationCatalogProperties(Set.of("admin", "pdp")).reservedNames())
                .containsExactlyInAnyOrder("admin", "pdp");
    }
}
