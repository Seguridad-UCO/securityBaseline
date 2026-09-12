package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.commons.IdentityEvidence;
import co.edu.uco.seguridad.pep.commons.ProxyTarget;
import co.edu.uco.seguridad.pep.ingress.CaptureHttpRequest;
import co.edu.uco.seguridad.pep.ingress.CapturedAccess;
import co.edu.uco.seguridad.pep.normalization.application.port.primary.dto.request.NormalizeAccessRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
final class HttpCaptureInteractor implements CaptureHttpRequest {

    private final RouteResolver routes;

    HttpCaptureInteractor(RouteResolver routes) {
        this.routes = routes;
    }

    @Override
    public Mono<CapturedAccess> execute(ServerWebExchange exchange) {
        return exchange.getPrincipal().cast(JwtAuthenticationToken.class).map(principal -> {
            String rawPath = exchange.getRequest().getURI().getRawPath();
            var route = routes.resolve(rawPath);
            String downstream = rawPath.substring(route.prefix().length());
            if (downstream.isEmpty()) downstream = "/";
            return new CapturedAccess(new NormalizeAccessRequest(exchange.getAttribute("pep.requestId"),
                    exchange.getAttribute("pep.correlationId"), Instant.now(), route.applicationId(),
                    route.environment(), downstream, exchange.getRequest().getMethod().name()),
                    new IdentityEvidence(principal.getToken().getTokenValue()),
                    new ProxyTarget(route.target(), downstream, route.forwardBearer(), route.forwardCookies()));
        }).switchIfEmpty(Mono.error(new EnforcementFailure(
                EnforcementFailure.Kind.UNAUTHENTICATED, "TOKEN_INVALID")));
    }
}
