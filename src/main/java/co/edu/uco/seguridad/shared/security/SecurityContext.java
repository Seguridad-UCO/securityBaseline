package co.edu.uco.seguridad.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

/**
 * El único camino para que un interactor pregunte quién hace la petición.
 *
 * <p>No hay una anotación {@code @AuthenticationPrincipal} en la firma del interactor a propósito:
 * los interactores implementan {@code ReactiveOperation<I, O>} con un solo parámetro, y ese parámetro
 * es el payload de la operación, no detalles de transporte. El principal viaja por el
 * {@code Reactor Context} que Spring Security ya propaga — este método solo le pone nombre al
 * mismo mecanismo que {@code ReactiveSecurityContextHolder} ofrece, para que ningún interactor
 * dependa directamente de las clases de Spring Security más allá de este punto.</p>
 */
public final class SecurityContext {

    private SecurityContext() {
    }

    public static Mono<PdpPrincipal> currentPrincipal() {
        return ReactiveSecurityContextHolder.getContext()
                .map(org.springframework.security.core.context.SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .cast(Jwt.class)
                .map(PdpPrincipal::from);
    }
}
