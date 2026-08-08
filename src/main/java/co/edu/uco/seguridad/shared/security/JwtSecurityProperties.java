package co.edu.uco.seguridad.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

/**
 * Configuración del emisor propio de JWT que valida esta línea base mientras no exista un IdP real
 * (ADR-0003). {@code secret} firma y valida en el mismo proceso — HMAC simétrico, no par de llaves —
 * porque no hay todavía un servicio de emisión separado. Externalizado en vez de fijo en código para
 * que cada ambiente tenga el suyo, igual que cualquier otro secreto reservado en Key Vault.
 *
 * @param secret  clave HMAC compartida, mínimo 256 bits (32 caracteres) para HS256
 * @param issuer  valor esperado del claim {@code iss}; un token de otro emisor se rechaza
 */
@ConfigurationProperties(prefix = "pdp.security.jwt")
public record JwtSecurityProperties(String secret, String issuer) {

    public JwtSecurityProperties {
        Objects.requireNonNull(secret, "se requiere el secreto de firma JWT (pdp.security.jwt.secret)");
        Objects.requireNonNull(issuer, "se requiere el emisor esperado (pdp.security.jwt.issuer)");
    }
}
