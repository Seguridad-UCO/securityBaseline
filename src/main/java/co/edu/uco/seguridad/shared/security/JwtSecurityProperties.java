package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.crosscutting.messages.RequiredArgumentMessages;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

/**
 * Configuración del emisor propio de JWT que valida esta línea base mientras no exista Keycloak
 * (ADR-018). HMAC simétrico porque emisor y validador son el mismo proceso.
 *
 * @param secret   clave HMAC compartida, mínimo 256 bits (32 caracteres) para HS256
 * @param issuer   valor esperado del claim {@code iss}; un token de otro emisor se rechaza
 * @param audience valor esperado del claim {@code aud}; un token emitido para otra aplicación del
 *                 mismo issuer se rechaza
 */
@ConfigurationProperties(prefix = "pdp.security.jwt")
public record JwtSecurityProperties(String secret, String issuer, String audience) {

    public JwtSecurityProperties {
        Objects.requireNonNull(secret, RequiredArgumentMessages.JWT_SECRET);
        Objects.requireNonNull(issuer, RequiredArgumentMessages.JWT_ISSUER);
        Objects.requireNonNull(audience, RequiredArgumentMessages.JWT_AUDIENCE);
    }
}
