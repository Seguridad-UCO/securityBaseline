package co.edu.uco.seguridad.shared.web.session;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Canal interno PEP→PDP: cambia una sesión BFF por el token del usuario, nunca expuesto al navegador. */
@RestController
@RequestMapping("/internal/v1/bff-session-token")
final class InternalBffSessionTokenController {
    private final ServerOAuth2AuthorizedClientRepository clients;
    InternalBffSessionTokenController(ServerOAuth2AuthorizedClientRepository clients) { this.clients = clients; }
    @GetMapping
    Mono<ResponseEntity<TokenResponse>> token(ServerWebExchange exchange, Authentication technicalIdentity) {
        return clients.loadAuthorizedClient("keycloak", technicalIdentity, exchange)
                .map(client -> ResponseEntity.ok().body(new TokenResponse(client.getAccessToken().getTokenValue())))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión BFF no activa.")));
    }
    record TokenResponse(String accessToken) {}
}
