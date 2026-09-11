package co.edu.uco.seguridad.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Confianza del JWT de evidencia del canal interno (HU-003, decisión D5, confirmada con el usuario:
 * el mismo Keycloak del BFF, solo JWKS — sin el modo HMAC de {@link JwtSecurityProperties}, y sin
 * exigir el claim {@code tenant} que {@link PdpPrincipal} sí exige, T1). Deuda consciente: cuando
 * HU-004 traiga issuer/JWKS por aplicación, este decoder se reemplaza por uno resuelto por
 * {@code application.id}, no por configuración estática.
 */
@ConfigurationProperties(prefix = "pdp.security.internal.evidence")
public record InternalEvidenceJwtProperties(String jwkSetUri, String issuer, String audience) {

    public InternalEvidenceJwtProperties {
    }
}
