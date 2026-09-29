package co.edu.uco.seguridad.shared.web.session;

import co.edu.uco.seguridad.shared.auth.service.KeycloakOidcSessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Logout de navegador: invalida la sesión BFF y luego completa el logout RP-initiated en Keycloak.
 */
@Controller
@Profile("keycloak")
public final class KeycloakLogoutController {

    public static final String KEYCLOAK_ID_TOKEN_ATTRIBUTE = "KEYCLOAK_ID_TOKEN";

    private final KeycloakOidcSessionService oidcSessionService;
    private final String frontendOrigin;

    KeycloakLogoutController(KeycloakOidcSessionService oidcSessionService,
                             @Value("${pdp.frontend.origin}") String frontendOrigin) {
        this.oidcSessionService = Objects.requireNonNull(oidcSessionService, "oidcSessionService");
        this.frontendOrigin = Objects.requireNonNull(frontendOrigin, "frontendOrigin");
    }

    @GetMapping("/api/v1/session/logout")
    Mono<Void> logout(ServerWebExchange exchange) {
        return exchange.getSession()
                .flatMap(session -> {
                    String idToken = session.getAttribute(KEYCLOAK_ID_TOKEN_ATTRIBUTE);
                    session.getAttributes().remove(KEYCLOAK_ID_TOKEN_ATTRIBUTE);
                    return session.invalidate()
                            .then(oidcSessionService.logoutUri(idToken, frontendOrigin))
                            .flatMap(uri -> redirect(exchange, uri));
                });
    }

    private Mono<Void> redirect(ServerWebExchange exchange, String uri) {
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FOUND);
        exchange.getResponse().getHeaders().setLocation(java.net.URI.create(uri));
        return exchange.getResponse().setComplete();
    }
}
