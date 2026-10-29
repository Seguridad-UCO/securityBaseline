package co.edu.uco.seguridad.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Renovación del token de la sesión BFF para el canal PEP (HU-030). Comparte prefijo con
 * {@link KeycloakSessionProperties}; se separa para no cambiar la firma de ese record.
 *
 * @param tokenRenewalSkew   margen antes de la expiración a partir del cual se renueva
 *                           ({@code pdp.security.session.token-renewal-skew}, por defecto 60 s)
 * @param tokenRenewalTimeout tope de espera de la renovación contra Keycloak
 *                           ({@code pdp.security.session.token-renewal-timeout}, por defecto 5 s)
 */
@ConfigurationProperties(prefix = "pdp.security.session")
public record SessionTokenRenewalProperties(
        @DefaultValue("60s") Duration tokenRenewalSkew,
        @DefaultValue("5s") Duration tokenRenewalTimeout) {

    public SessionTokenRenewalProperties {
        if (tokenRenewalSkew != null && tokenRenewalSkew.isNegative()) {
            throw new IllegalArgumentException("El margen de renovación del token no puede ser negativo.");
        }
    }
}
