package co.edu.uco.seguridad.pdp.tenants.infrastructure.properties;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code seed} llega directo de {@code application.properties} — nunca se asume que Spring siempre
 * inyecta un mapa no nulo cuando la clave {@code pdp.tenants} está ausente.
 */
class TenantCatalogPropertiesTests {

    @Test
    void a_missing_seed_becomes_an_empty_catalog_instead_of_a_null_map() {
        assertThat(new TenantCatalogProperties(null).seed()).isEmpty();
        assertThat(new TenantCatalogProperties(null).ids()).isEmpty();
    }

    @Test
    void ids_lists_every_seeded_tenant() {
        var properties = new TenantCatalogProperties(Map.of("universidad-uco", "ACTIVE", "tenant-a", "ACTIVE"));

        assertThat(properties.ids()).containsExactlyInAnyOrder("universidad-uco", "tenant-a");
    }
}
