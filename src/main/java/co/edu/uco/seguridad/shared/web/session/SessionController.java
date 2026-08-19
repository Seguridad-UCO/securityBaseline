package co.edu.uco.seguridad.shared.web.session;

import co.edu.uco.seguridad.shared.security.SecurityContext;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

/** Punto de lectura de la sesión BFF. El navegador solo recibe identidad y tenant, nunca el access token. */
@RestController
@RequestMapping("/api/v1/session")
final class SessionController {
    @GetMapping
    Mono<ApiResponse<SessionResponse>> current(ServerWebExchange exchange) {
        Mono<CsrfToken> csrf = exchange.getAttribute(CsrfToken.class.getName());
        Mono<Void> bootstrapCsrfCookie = csrf == null ? Mono.empty() : csrf.then();
        return bootstrapCsrfCookie.then(ReactiveSecurityContextHolder.getContext())
                .map(context -> context.getAuthentication().getPrincipal())
                .flatMap(authenticated -> {
                    if (authenticated instanceof LocalUserPrincipal user) {
                        return Mono.just(ApiResponse.success("SESSION_ACTIVE", "Authenticated session",
                                new SessionResponse(user.subject(), user.name(), user.email(), user.tenantId().value()),
                                CorrelationWebFilter.context(exchange)));
                    }
                    return SecurityContext.currentPrincipal().map(principal -> ApiResponse.success("SESSION_ACTIVE",
                            "Authenticated session", new SessionResponse(principal.subject(), "", "", principal.tenantId().value()),
                            CorrelationWebFilter.context(exchange)));
                });
    }
}
