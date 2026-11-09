package co.edu.uco.seguridad.pep.starter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Filtro fail-closed instalado dentro de la aplicación protegida. Si el PEP, PDP o una política no
 * puede confirmar un ALLOW, la ruta no llega al handler de negocio.
 */
// Deja que CorsWebFilter (HIGHEST_PRECEDENCE) resuelva el preflight y agregue los encabezados
// incluso cuando esta capa vaya a responder 401/403.
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
final class PepEnforcementWebFilter implements WebFilter {
    private final PepEnforcementProperties properties;
    private final WebClient client;
    private final List<PathPattern> publicPaths;

    PepEnforcementWebFilter(PepEnforcementProperties properties, WebClient client) {
        this.properties = properties;
        this.client = client;
        PathPatternParser parser = new PathPatternParser();
        this.publicPaths = properties.publicPaths().stream().map(parser::parse).toList();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (CorsUtils.isPreFlightRequest(exchange.getRequest())) return chain.filter(exchange);
        if (publicPaths.stream().anyMatch(pattern -> pattern.matches(exchange.getRequest().getPath()))) {
            return chain.filter(exchange);
        }
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String session = exchange.getRequest().getHeaders().getFirst(HttpHeaders.COOKIE);
        boolean bearer = exchange.getRequest().getHeaders().getOrEmpty(HttpHeaders.AUTHORIZATION).size() == 1
                && authorization != null && authorization.startsWith("Bearer ") && authorization.length() > 7
                && !authorization.substring(7).isBlank();
        if (!bearer && (session == null || !session.contains("SECURITY_BASELINE_SESSION="))) {
            return write(exchange, HttpStatus.UNAUTHORIZED, "TOKEN_INVALID");
        }
        String requestId = UUID.randomUUID().toString();
        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
        if (correlationId == null || !correlationId.matches("[A-Za-z0-9_.:-]{1,128}")) correlationId = requestId;
        String finalCorrelationId = correlationId;
        return client.post().uri("/internal/v1/embedded-access-decisions")
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    if (bearer) headers.set(HttpHeaders.AUTHORIZATION, authorization);
                    if (session != null) headers.set(HttpHeaders.COOKIE, session);
                    headers.set("X-Application-Credential", properties.applicationCredential());
                    headers.set("X-Request-Id", requestId);
                    headers.set("X-Correlation-Id", finalCorrelationId);
                })
                .bodyValue(Map.of("applicationName", properties.applicationName(), "environment", properties.environment(),
                        "path", exchange.getRequest().getURI().getRawPath(), "method", exchange.getRequest().getMethod().name()))
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        String decisionId = response.headers().asHttpHeaders().getFirst("X-Decision-Id");
                        return response.releaseBody().thenReturn(new Decision(true, decisionId, null, null));
                    }
                    HttpStatus status = switch (response.statusCode().value()) {
                        case 401 -> HttpStatus.UNAUTHORIZED;
                        case 403 -> HttpStatus.FORBIDDEN;
                        default -> HttpStatus.SERVICE_UNAVAILABLE;
                    };
                    return response.releaseBody().thenReturn(new Decision(false, null, status, switch (status) {
                        case UNAUTHORIZED -> "TOKEN_INVALID";
                        case FORBIDDEN -> "ACCESS_DENIED";
                        default -> "PEP_UNAVAILABLE";
                    }));
                })
                .onErrorResume(error -> Mono.just(new Decision(false, null, HttpStatus.SERVICE_UNAVAILABLE,
                        "PEP_UNAVAILABLE")))
                .flatMap(decision -> {
                    if (!decision.allowed()) return write(exchange, decision.status(), decision.code());
                    if (decision.decisionId() != null) {
                        exchange.getResponse().getHeaders().set("X-Decision-Id", decision.decisionId());
                    }
                    return chain.filter(exchange);
                });
    }

    private static Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String code) {
        if (exchange.getResponse().isCommitted()) return Mono.empty();
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        exchange.getResponse().getHeaders().setCacheControl("no-store");
        if (status == HttpStatus.UNAUTHORIZED)
            exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, status.getReasonPhrase());
        problem.setTitle(code);
        problem.setType(URI.create("urn:security-pep:error:" + code.toLowerCase(java.util.Locale.ROOT)));
        problem.setProperty("code", code);
        byte[] bytes = ("{\"type\":\"" + problem.getType() + "\",\"title\":\"" + code
                + "\",\"status\":" + status.value() + ",\"detail\":\"" + status.getReasonPhrase()
                + "\",\"code\":\"" + code + "\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private record Decision(boolean allowed, String decisionId, HttpStatus status, String code) {
    }
}
