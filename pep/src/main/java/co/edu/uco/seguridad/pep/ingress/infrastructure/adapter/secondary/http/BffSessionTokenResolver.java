package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.secondary.http;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.ingress.application.port.secondary.PdpServiceTokenProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Resuelve la cookie BFF únicamente por el canal técnico PEP→PDP.
 */
public final class BffSessionTokenResolver {
    private final WebClient pdp;
    private final PdpServiceTokenProvider technical;

    public BffSessionTokenResolver(WebClient pdp, PdpServiceTokenProvider technical) {
        this.pdp = pdp;
        this.technical = technical;
    }

    public Mono<String> resolve(ServerWebExchange exchange) {
        String cookie = exchange.getRequest().getHeaders().getFirst(HttpHeaders.COOKIE);
        if (cookie == null || !cookie.contains("SECURITY_BASELINE_SESSION=")) return Mono.empty();
        return technical.token().flatMap(token -> pdp.get().uri("/internal/v1/bff-session-token")
                .headers(h -> {
                    h.setBearerAuth(token);
                    h.set(HttpHeaders.COOKIE, cookie);
                })
                .retrieve().bodyToMono(TokenResponse.class).map(TokenResponse::accessToken)
                .onErrorMap(error -> new EnforcementFailure(EnforcementFailure.Kind.UNAUTHENTICATED, "TOKEN_INVALID")));
    }

    private record TokenResponse(String accessToken) {
    }
}
