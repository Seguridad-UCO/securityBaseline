package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Objects;

/**
 * El sujeto autenticado: quién es, de qué tenant y con qué token, tomados del token, nunca del
 * cuerpo ni la query (ADR-018). {@code tenant} es un claim propio; reutiliza el constructor de
 * {@link TenantId} para que un claim inválido reporte el mismo error que un {@code TenantId}
 * inválido en cualquier otro punto. {@code tokenId} (jti) no se usa todavía — queda disponible para
 * cuando exista revocación por Redis.
 */
public record PdpPrincipal(TenantId tenantId, String subject, String tokenId) {

    public PdpPrincipal {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(subject, RequiredArgumentMessages.SUBJECT);
        Objects.requireNonNull(tokenId, RequiredArgumentMessages.JWT_ID);
    }

    public static PdpPrincipal from(Jwt jwt) {
        return new PdpPrincipal(new TenantId(jwt.getClaimAsString("tenant")), jwt.getSubject(), jwt.getId());
    }

    /**
     * Compatibilidad temporal para requests que todavía lleguen con un principal OIDC antes de que
     * el BFF persista el {@link LocalUserPrincipal}. El tenant sigue siendo local; si este camino
     * se usa, el token debe traer el claim mientras termina la transición.
     */
    public static PdpPrincipal from(OidcUser user) {
        String sessionId = user.getClaimAsString("sid");
        if (sessionId == null || sessionId.isBlank()) sessionId = user.getClaimAsString("jti");
        return new PdpPrincipal(new TenantId(user.getClaimAsString("tenant")), user.getSubject(), sessionId);
    }
}
