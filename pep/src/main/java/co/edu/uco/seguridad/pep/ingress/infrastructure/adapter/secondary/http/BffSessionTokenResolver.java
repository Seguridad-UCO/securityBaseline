package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.secondary.http;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.ingress.application.port.secondary.PdpServiceTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Resuelve la cookie BFF únicamente por el canal técnico PEP→PDP.
 */
public final class BffSessionTokenResolver {
    private static final Logger LOG = LoggerFactory.getLogger(BffSessionTokenResolver.class);

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
                .exchangeToMono(this::classify)
                .onErrorMap(error -> !(error instanceof EnforcementFailure), error -> unavailable()));
    }

    private Mono<String> classify(ClientResponse response) {
        HttpStatusCode status = response.statusCode();
        if (status.value() == HttpStatus.UNAUTHORIZED.value()) {
            return response.releaseBody().then(Mono.error(
                    new EnforcementFailure(EnforcementFailure.Kind.UNAUTHENTICATED, "TOKEN_INVALID")));
        }
        if (!status.is2xxSuccessful()) {
            if (status.is4xxClientError()) {
                LOG.atWarn().addKeyValue("event.name", "pep.bff-session.pdp-rejected")
                        .addKeyValue("pdp.status", status.value())
                        .log("pep.bff-session.pdp-rejected: el PDP rechazó la consulta de sesión BFF");
            }
            return response.releaseBody().then(Mono.error(unavailable()));
        }
        return response.bodyToMono(TokenResponse.class)
                .filter(body -> body.accessToken() != null && !body.accessToken().isBlank())
                .map(TokenResponse::accessToken)
                .switchIfEmpty(Mono.error(unavailable()));
    }

    private static EnforcementFailure unavailable() {
        return new EnforcementFailure(EnforcementFailure.Kind.UNAVAILABLE, "BFF_SESSION_UNAVAILABLE");
    }

    private record TokenResponse(String accessToken) {
    }
}
