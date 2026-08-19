package co.edu.uco.seguridad.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configura la cookie de sesión del BFF cuando el perfil keycloak está activo. */
@ConfigurationProperties(prefix = "pdp.security.session")
public record KeycloakSessionProperties(boolean secureCookies) {
}
