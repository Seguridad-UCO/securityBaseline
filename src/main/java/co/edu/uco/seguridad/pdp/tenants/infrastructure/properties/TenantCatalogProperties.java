package co.edu.uco.seguridad.pdp.tenants.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Catálogo de inquilinos externalizado. Los identificadores de inquilino son configuración, no secretos, por lo
 * que pertenecen a {@code application.properties} y nunca a Key Vault.
 *
 * @param seed id de inquilino al nombre de {@code TenantStatus}, usado por el adaptador dummy hasta que exista
 *             el almacén real de inquilinos
 */
@ConfigurationProperties(prefix = "pdp.tenants")
public record TenantCatalogProperties(Map<String, String> seed) {

    public TenantCatalogProperties {
        seed = seed == null ? Map.of() : Map.copyOf(seed);
    }

    public List<String> ids() {
        return List.copyOf(seed.keySet());
    }
}
