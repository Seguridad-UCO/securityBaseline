package co.edu.uco.seguridad.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import reactor.core.publisher.Mono;

/**
 * El único camino para que un interactor pregunte quién hace la petición. Sin
 * {@code @AuthenticationPrincipal} a propósito: {@code ReactiveOperation<I, O>} toma un solo
 * parámetro, el payload de la operación, no detalles de transporte.
 */
public final class SecurityContext {

    private SecurityContext() {
    }

    public static Mono<PdpPrincipal> currentPrincipal() {
        return ReactiveSecurityContextHolder.getContext()
                .map(org.springframework.security.core.context.SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .flatMap(principal -> {
                    if (principal instanceof Jwt jwt) return Mono.just(PdpPrincipal.from(jwt));
                    if (principal instanceof OidcUser user) return Mono.just(PdpPrincipal.from(user));
                    if (principal instanceof LocalUserPrincipal user) return Mono.just(new PdpPrincipal(user.tenantId(), user.subject(), user.userId()));
                    return Mono.error(new IllegalStateException("principal de seguridad no soportado"));
                });
    }
}
