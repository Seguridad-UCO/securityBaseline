package co.edu.uco.seguridad.shared.auth.web;

import co.edu.uco.seguridad.shared.auth.service.OidcAuthorizationFlowService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Controller
@Profile("keycloak")
public final class KeycloakRegistrationController {

    private final OidcAuthorizationFlowService authorizationFlowService;

    KeycloakRegistrationController(OidcAuthorizationFlowService authorizationFlowService) {
        this.authorizationFlowService = authorizationFlowService;
    }

    @GetMapping("/oauth2/authorization/keycloak/register")
    Mono<Void> beginRegistration(ServerWebExchange exchange) {
        return authorizationFlowService.beginRegistration(exchange);
    }
}
