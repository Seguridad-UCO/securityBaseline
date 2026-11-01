package co.edu.uco.seguridad.shared.web.session;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import co.edu.uco.seguridad.shared.auth.model.BffSessionExpiredException;
import co.edu.uco.seguridad.shared.auth.model.TokenRenewalUnavailableException;
import co.edu.uco.seguridad.shared.auth.service.BffSessionTokenRenewer;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Canal interno PEP→PDP: cambia una sesión BFF por el token del usuario, nunca expuesto al
 * navegador. Delega en {@link BffSessionTokenRenewer} (HU-030), que renueva el token si está por vencer;
 * solo existe con el perfil {@code keycloak}, igual que este controlador.
 */
@RestController
@RequestMapping("/internal/v1/bff-session-token")
@Profile("keycloak")
final class InternalBffSessionTokenController {
    private static final String SESSION_NOT_ACTIVE = "Sesión BFF no activa.";
    private static final String RENEWAL_UNAVAILABLE = "No se pudo renovar el token de la sesión BFF.";

    private final BffSessionTokenRenewer renewer;
    private final WebSessionServerSecurityContextRepository bffSecurityContext =
            new WebSessionServerSecurityContextRepository();

    InternalBffSessionTokenController(BffSessionTokenRenewer renewer) {
        this.renewer = Objects.requireNonNull(renewer, "renewer");
    }

    @GetMapping
    Mono<ResponseEntity<TokenResponse>> token(ServerWebExchange exchange) {
        // Esta ruta se autentica con la identidad técnica del PEP, pero el cliente OIDC está
        // asociado al principal BFF guardado en la misma WebSession. Usar el principal técnico
        // aquí siempre daba "Sesión BFF no activa" aunque la cookie fuera válida.
        return bffSecurityContext.load(exchange)
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .flatMap(bffPrincipal -> renewer.validAccessToken(exchange, authorizedClientPrincipal(bffPrincipal)))
                .map(accessToken -> ResponseEntity.ok().body(new TokenResponse(accessToken)))
                .onErrorMap(BffSessionExpiredException.class,
                        error -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, SESSION_NOT_ACTIVE))
                .onErrorMap(TokenRenewalUnavailableException.class,
                        error -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, RENEWAL_UNAVAILABLE))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, SESSION_NOT_ACTIVE)));
    }

    /**
     * El cliente autorizado se almacena durante el callback OIDC con el {@code sub} de Keycloak.
     * La sesión BFF sustituye después el principal OIDC por {@link LocalUserPrincipal}; usar ese
     * record directamente hace que {@code Authentication#getName()} sea su {@code toString()}, por
     * lo que el repositorio ya no encuentra el token del mismo usuario.
     */
    private static Authentication authorizedClientPrincipal(Authentication bffPrincipal) {
        if (bffPrincipal.getPrincipal() instanceof LocalUserPrincipal user) {
            return new UsernamePasswordAuthenticationToken(user.subject(), null, java.util.List.of());
        }
        return bffPrincipal;
    }

    record TokenResponse(String accessToken) {
    }
}
