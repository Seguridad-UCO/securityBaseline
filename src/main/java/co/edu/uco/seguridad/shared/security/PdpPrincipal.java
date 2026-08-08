package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;

/**
 * El sujeto autenticado de una petición: quién es y de qué tenant, ambos tomados del token — nunca
 * del cuerpo ni de la query (ADR-0003).
 *
 * <p>{@code tenant} es un claim propio, no estándar de JWT; un emisor real (Keycloak) lo poblará
 * igual que el emisor propio de esta etapa. Reutiliza el constructor de {@link TenantId} para
 * validar el claim: un tenant ausente o mal formado en el token se reporta con el mismo código de
 * error que un {@code TenantId} inválido en cualquier otro punto de entrada, en vez de inventar una
 * excepción paralela para el mismo problema.</p>
 */
public record PdpPrincipal(TenantId tenantId, String subject) {

    public PdpPrincipal {
        Objects.requireNonNull(tenantId, "se requiere id de inquilino");
        Objects.requireNonNull(subject, "se requiere sujeto");
    }

    public static PdpPrincipal from(Jwt jwt) {
        return new PdpPrincipal(new TenantId(jwt.getClaimAsString("tenant")), jwt.getSubject());
    }
}
