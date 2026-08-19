package co.edu.uco.seguridad.pdp.applications.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

/**
 * Nombres que la plataforma reserva para sí misma. Externalizado porque la lista es política que
 * cambia con el despliegue, no una decisión que el código deba fijar.
 *
 * @param reservedNames nombres de aplicación que ningún inquilino puede registrar
 */
@ConfigurationProperties(prefix = "pdp.applications")
public record ApplicationCatalogProperties(Set<String> reservedNames) {

    public ApplicationCatalogProperties {
        reservedNames = reservedNames == null ? Set.of() : Set.copyOf(reservedNames);
    }
}
