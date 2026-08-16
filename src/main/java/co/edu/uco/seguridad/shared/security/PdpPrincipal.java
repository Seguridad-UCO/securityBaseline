package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.crosscutting.messages.RequiredArgumentMessages;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;

/**
 * El sujeto autenticado: quién es y de qué tenant, tomados del token, nunca del cuerpo ni la query
 * (ADR-018). {@code tenant} es un claim propio; reutiliza el constructor de {@link TenantId} para
 * que un claim inválido reporte el mismo error que un {@code TenantId} inválido en cualquier otro punto.
 */
public record PdpPrincipal(TenantId tenantId, String subject) {

    public PdpPrincipal {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(subject, RequiredArgumentMessages.SUBJECT);
    }

    public static PdpPrincipal from(Jwt jwt) {
        return new PdpPrincipal(new TenantId(jwt.getClaimAsString("tenant")), jwt.getSubject());
    }
}
