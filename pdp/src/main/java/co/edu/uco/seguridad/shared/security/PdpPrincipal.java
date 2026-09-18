package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.mfa.AuthenticationContextEvidence;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Objects;
import java.util.Optional;

/**
 * El sujeto autenticado: quién es, de qué tenant y con qué token, tomados del token, nunca del
 * cuerpo ni la query (ADR-018). {@code tenant} es un claim propio; reutiliza el constructor de
 * {@link TenantId} para que un claim inválido reporte el mismo error que un {@code TenantId}
 * inválido en cualquier otro punto. {@code tokenId} (jti) no se usa todavía — queda disponible para
 * cuando exista revocación por Redis. {@code userId} (HU-008) es el identificador interno ya
 * resuelto por el BFF en el primer login — presente solo para {@link LocalUserPrincipal}; los
 * caminos {@code Jwt}/{@code OidcUser} son compatibilidad temporal y no lo tienen todavía.
 */
public record PdpPrincipal(TenantId tenantId, String subject, String tokenId, Optional<UserId> userId,
        AuthenticationContextEvidence authenticationContext) {

    public PdpPrincipal {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(subject, RequiredArgumentMessages.SUBJECT);
        Objects.requireNonNull(tokenId, RequiredArgumentMessages.JWT_ID);
        Objects.requireNonNull(userId, RequiredArgumentMessages.PRINCIPAL_USER_ID);
        Objects.requireNonNull(authenticationContext, RequiredArgumentMessages.AUTHENTICATION_CONTEXT_EVIDENCE);
    }

    public static PdpPrincipal from(Jwt jwt) {
        return new PdpPrincipal(new TenantId(jwt.getClaimAsString("tenant")), jwt.getSubject(), jwt.getId(),
                Optional.empty(), AuthenticationContextEvidence.from(jwt));
    }

    /**
     * Compatibilidad temporal para requests que todavía lleguen con un principal OIDC antes de que
     * el BFF persista el {@link LocalUserPrincipal}. El tenant sigue siendo local; si este camino
     * se usa, el token debe traer el claim mientras termina la transición.
     */
    public static PdpPrincipal from(OidcUser user) {
        String sessionId = user.getClaimAsString("sid");
        if (sessionId == null || sessionId.isBlank()) sessionId = user.getClaimAsString("jti");
        return new PdpPrincipal(new TenantId(user.getClaimAsString("tenant")), user.getSubject(), sessionId,
                Optional.empty(), AuthenticationContextEvidence.from(user));
    }
}
