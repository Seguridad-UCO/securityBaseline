package co.edu.uco.seguridad.pdp.identity.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tenant al que se asigna un usuario nuevo en su primer login, antes de que un administrador lo
 * reasigne explícitamente (ver {@code AssignTenantUseCase}).
 */
@ConfigurationProperties(prefix = "pdp.identity")
public record IdentityProvisioningProperties(String defaultTenantId) {
}
