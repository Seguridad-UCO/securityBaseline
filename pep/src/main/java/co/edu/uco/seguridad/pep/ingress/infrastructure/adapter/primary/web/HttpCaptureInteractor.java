package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.commons.IdentityEvidence;
import co.edu.uco.seguridad.pep.commons.ProxyTarget;
import co.edu.uco.seguridad.pep.ingress.CaptureHttpRequest;
import co.edu.uco.seguridad.pep.ingress.CapturedAccess;
import co.edu.uco.seguridad.pep.normalization.application.port.primary.dto.request.NormalizeAccessRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.secondary.http.BffSessionTokenResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
final class HttpCaptureInteractor implements CaptureHttpRequest {

    private final RouteResolver routes;
    private final BffSessionTokenResolver bffSession;

    HttpCaptureInteractor(RouteResolver routes, BffSessionTokenResolver bffSession) {
        this.routes = routes;
        this.bffSession = bffSession;
    }

    @Override
    public Mono<CapturedAccess> execute(ServerWebExchange exchange) {
        String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
        Mono<String> evidence = authorization != null && authorization.startsWith("Bearer ") && authorization.length() > 7
                ? Mono.just(authorization.substring(7)) : bffSession.resolve(exchange);
        return evidence.map(bearer -> {
            String rawPath = exchange.getRequest().getURI().getRawPath();
            var route = routes.resolve(rawPath);
            String downstream = rawPath.substring(route.prefix().length());
            if (downstream.isEmpty()) downstream = "/";
            return new CapturedAccess(new NormalizeAccessRequest(exchange.getAttribute("pep.requestId"),
                    exchange.getAttribute("pep.correlationId"), Instant.now(), route.applicationId(),
                    route.environment(), downstream, exchange.getRequest().getMethod().name()),
                    new IdentityEvidence(bearer),
                    new ProxyTarget(route.target(), downstream, route.forwardBearer(), route.forwardCookies()));
        }).switchIfEmpty(Mono.error(new EnforcementFailure(
                EnforcementFailure.Kind.UNAUTHENTICATED, "TOKEN_INVALID")));
    }
}
