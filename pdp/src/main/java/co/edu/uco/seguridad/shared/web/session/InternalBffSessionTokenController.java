package co.edu.uco.seguridad.shared.web.session;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Canal interno PEP→PDP: cambia una sesión BFF por el token del usuario, nunca expuesto al
 * navegador. {@code ServerOAuth2AuthorizedClientRepository} solo existe cuando el perfil
 * {@code keycloak} registra el cliente OAuth2 — mismo guard que {@code KeycloakLoginController}
 * (sin él, cualquier perfil sin Keycloak activo falla al arrancar el contexto).
 */
@RestController
@RequestMapping("/internal/v1/bff-session-token")
@Profile("keycloak")
final class InternalBffSessionTokenController {
    private final ServerOAuth2AuthorizedClientRepository clients;
    private final WebSessionServerSecurityContextRepository bffSecurityContext =
            new WebSessionServerSecurityContextRepository();

    InternalBffSessionTokenController(ServerOAuth2AuthorizedClientRepository clients) {
        this.clients = clients;
    }

    @GetMapping
    Mono<ResponseEntity<TokenResponse>> token(ServerWebExchange exchange) {
        // Esta ruta se autentica con la identidad técnica del PEP, pero el cliente OIDC está
        // asociado al principal BFF guardado en la misma WebSession. Usar el principal técnico
        // aquí siempre daba "Sesión BFF no activa" aunque la cookie fuera válida.
        return bffSecurityContext.load(exchange)
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .flatMap(bffPrincipal -> clients.<OAuth2AuthorizedClient>loadAuthorizedClient(
                        "keycloak", authorizedClientPrincipal(bffPrincipal), exchange))
                .map(client -> ResponseEntity.ok().body(new TokenResponse(client.getAccessToken().getTokenValue())))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión BFF no activa.")));
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
