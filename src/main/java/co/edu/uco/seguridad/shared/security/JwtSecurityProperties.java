package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.crosscutting.messages.RequiredArgumentMessages;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

/**
 * Configuración del validador JWT (ADR-020). Dos modos, exactamente uno debe estar configurado:
 * {@code secret} (HMAC, el emisor propio original — sigue vivo solo para pruebas/desarrollo local
 * sin depender de un Keycloak real) o {@code jwkSetUri} (RS256 contra las llaves públicas de
 * Keycloak, usado en todo ambiente desplegado). {@code jwtDecoder()} en {@code SecurityConfiguration}
 * decide cuál construir.
 *
 * @param secret    clave HMAC compartida, mínimo 256 bits (32 caracteres) para HS256 — modo prueba
 * @param jwkSetUri endpoint JWKS de Keycloak (p. ej. {@code .../realms/pdp/protocol/openid-connect/certs}) — modo real
 * @param issuer    valor esperado del claim {@code iss}; un token de otro emisor se rechaza
 * @param audience  valor esperado del claim {@code aud}; un token emitido para otra aplicación del
 *                  mismo issuer se rechaza
 */
@ConfigurationProperties(prefix = "pdp.security.jwt")
public record JwtSecurityProperties(String secret, String jwkSetUri, String issuer, String audience) {

    public JwtSecurityProperties {
        Objects.requireNonNull(issuer, RequiredArgumentMessages.JWT_ISSUER);
        Objects.requireNonNull(audience, RequiredArgumentMessages.JWT_AUDIENCE);
        boolean hasSecret = secret != null && !secret.isBlank();
        boolean hasJwkSetUri = jwkSetUri != null && !jwkSetUri.isBlank();
        if (hasSecret == hasJwkSetUri) {
            throw new IllegalStateException(RequiredArgumentMessages.JWT_EXACTLY_ONE_MODE);
        }
    }

    public boolean usesJwks() {
        return jwkSetUri != null && !jwkSetUri.isBlank();
    }
}
