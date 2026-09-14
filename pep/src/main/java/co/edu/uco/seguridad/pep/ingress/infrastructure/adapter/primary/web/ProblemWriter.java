package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.Locale;

@Component
public final class ProblemWriter {

    private static final Logger LOG = LoggerFactory.getLogger(ProblemWriter.class);

    private final ObjectMapper mapper;

    public ProblemWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Mono<Void> write(ServerWebExchange exchange, EnforcementFailure failure) {
        if (exchange.getResponse().isCommitted()) return Mono.error(failure);
        HttpStatus status = switch (failure.kind()) {
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case UNKNOWN_ROUTE -> HttpStatus.NOT_FOUND;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case DENIED -> HttpStatus.FORBIDDEN;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE;
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case BAD_GATEWAY -> HttpStatus.BAD_GATEWAY;
            case GATEWAY_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            case UNSUPPORTED -> HttpStatus.NOT_IMPLEMENTED;
        };
        // Remove stale downstream entity headers before rendering a local error.
        var headers = exchange.getResponse().getHeaders();
        headers.remove(HttpHeaders.CONTENT_LENGTH);
        headers.remove(HttpHeaders.CONTENT_ENCODING);
        headers.remove(HttpHeaders.ETAG);
        headers.remove(HttpHeaders.SET_COOKIE);
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        headers.setCacheControl("no-store");
        if (status == HttpStatus.UNAUTHORIZED) headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        if (status == HttpStatus.TOO_MANY_REQUESTS) headers.set(HttpHeaders.RETRY_AFTER, "1");
        exchange.getResponse().setStatusCode(status);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, status.getReasonPhrase());
        detail.setTitle(failure.code());
        detail.setType(URI.create("urn:security-pep:error:" + failure.code().toLowerCase(Locale.ROOT)));
        detail.setProperty("code", failure.code());
        detail.setProperty("requestId", exchange.getAttribute("pep.requestId"));
        detail.setProperty("correlationId", exchange.getAttribute("pep.correlationId"));
        if (failure.decisionId() != null) {
            detail.setProperty("decisionId", failure.decisionId());
            headers.set("X-Decision-Id", failure.decisionId());
            exchange.getAttributes().put("pep.decisionId", failure.decisionId());
        }
        if (failure.kind() == EnforcementFailure.Kind.UNAVAILABLE) {
            String requestId = exchange.getAttribute("pep.requestId");
            String correlationId = exchange.getAttribute("pep.correlationId");
            LOG.atWarn().addKeyValue("event.name", "pep.enforcement.unavailable")
                    .addKeyValue("requestId", requestId)
                    .addKeyValue("correlationId", correlationId)
                    .addKeyValue("failure.code", failure.code())
                    .addKeyValue("decisionId", failure.decisionId())
                    .log("Enforcement no disponible");
        }
        return exchange.getResponse().writeWith(Mono.fromSupplier(() ->
                exchange.getResponse().bufferFactory().wrap(mapper.writeValueAsBytes(detail))));
    }
}
