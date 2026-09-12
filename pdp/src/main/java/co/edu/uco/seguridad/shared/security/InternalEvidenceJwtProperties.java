package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

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
        Objects.requireNonNull(jwkSetUri, RequiredArgumentMessages.INTERNAL_EVIDENCE_JWK_SET_URI);
        Objects.requireNonNull(issuer, RequiredArgumentMessages.INTERNAL_EVIDENCE_ISSUER);
        Objects.requireNonNull(audience, RequiredArgumentMessages.INTERNAL_EVIDENCE_AUDIENCE);
    }
}
