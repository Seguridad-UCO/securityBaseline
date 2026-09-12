package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.ingress.infrastructure.properties.IngressProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import static co.edu.uco.seguridad.pep.commons.EnforcementFailure.Kind.*;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
final class IngressLimitsFilter implements WebFilter {

    private final IngressProperties properties;
    private final ProblemWriter problems;
    private final Semaphore concurrency;
    private final Map<String, Bucket> buckets = new HashMap<>();
    private final MeterRegistry metrics;
    private long lastSweep;
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(IngressLimitsFilter.class);

    IngressLimitsFilter(IngressProperties properties, ProblemWriter problems, MeterRegistry metrics) {
        this.properties = properties;
        this.problems = problems;
        this.metrics = metrics;
        this.concurrency = new Semaphore(properties.maxConcurrent());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestId = UUID.randomUUID().toString();
        String correlation = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
        if (correlation == null || !correlation.matches("[A-Za-z0-9_.:-]{1,128}"))
            correlation = UUID.randomUUID().toString();
        exchange.getAttributes().put("pep.requestId", requestId);
        exchange.getAttributes().put("pep.correlationId", correlation);
        exchange.getResponse().getHeaders().set("X-Request-Id", requestId);
        exchange.getResponse().getHeaders().set("X-Correlation-Id", correlation);
        long start = System.nanoTime();
        return Mono.defer(() -> {
                    if (HttpMethod.GET.equals(exchange.getRequest().getMethod())
                            && Set.of("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness", "/actuator/prometheus")
                            .contains(exchange.getRequest().getPath().value())) return chain.filter(exchange);
                    validateRequest(exchange);
                    String ip = exchange.getRequest().getRemoteAddress() == null ? "unknown"
                            : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
                    if (!take(ip)) throw new EnforcementFailure(RATE_LIMITED, "RATE_LIMIT_EXCEEDED");
                    if (!concurrency.tryAcquire()) throw new EnforcementFailure(UNAVAILABLE, "CAPACITY_EXCEEDED");
                    return Mono.defer(() -> chain.filter(exchange)).doFinally(signal -> concurrency.release());
                }).onErrorResume(EnforcementFailure.class, failure -> problems.write(exchange, failure))
                .onErrorResume(org.springframework.security.authentication.AuthenticationServiceException.class,
                        error -> problems.write(exchange, new EnforcementFailure(UNAVAILABLE, "IDENTITY_UNAVAILABLE")))
                .onErrorResume(error -> {
                    LOG.atError().addKeyValue("event.name", "http.request.failed")
                            .addKeyValue("requestId", requestId)
                            .addKeyValue("error.type", error.getClass().getSimpleName()).log("Petición fallida");
                    return problems.write(exchange, new EnforcementFailure(UNAVAILABLE, "REQUEST_FAILED"));
                })
                .doFinally(signal -> {
                    int status = exchange.getResponse().getStatusCode() == null ? 200 : exchange.getResponse().getStatusCode().value();
                    metrics.timer("pep.http.duration", "status", Integer.toString(status))
                            .record(System.nanoTime() - start, java.util.concurrent.TimeUnit.NANOSECONDS);
                    LOG.atInfo().addKeyValue("event.name", "pep.enforcement.completed")
                            .addKeyValue("requestId", requestId)
                            .addKeyValue("correlationId", exchange.<String>getAttribute("pep.correlationId"))
                            .addKeyValue("decisionId", exchange.<String>getAttribute("pep.decisionId"))
                            .addKeyValue("http.response.status_code", status).log("Enforcement finalizado");
                });
    }

    private void validateRequest(ServerWebExchange exchange) {
        var request = exchange.getRequest();
        if (!Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS").contains(request.getMethod().name())
                || request.getHeaders().containsHeader("Upgrade")
                || request.getHeaders().getOrEmpty("Accept").stream().anyMatch(v -> v.contains("text/event-stream"))) {
            throw new EnforcementFailure(UNSUPPORTED, "UNSUPPORTED_PROTOCOL");
        }
        if (request.getHeaders().getOrEmpty("Authorization").size() > 1) {
            throw new EnforcementFailure(INVALID_REQUEST, "AMBIGUOUS_AUTHORIZATION");
        }
        if (request.getHeaders().getContentLength() > properties.maxBodyBytes()) {
            throw new EnforcementFailure(TOO_LARGE, "REQUEST_TOO_LARGE");
        }
    }

    private synchronized boolean take(String key) {
        long now = System.nanoTime();
        if (now - lastSweep > 1_000_000_000L) {
            buckets.values().removeIf(b -> now - b.lastSeen > 60_000_000_000L);
            lastSweep = now;
        }
        Bucket bucket = buckets.get(key);
        if (bucket == null) {
            if (buckets.size() >= properties.maxRateKeys()) return false;
            bucket = new Bucket(properties.burst(), now);
            buckets.put(key, bucket);
        }
        bucket.tokens = Math.min(properties.burst(), bucket.tokens
                + (now - bucket.lastSeen) / 1_000_000_000.0 * properties.requestsPerSecond());
        bucket.lastSeen = now;
        if (bucket.tokens < 1) return false;
        bucket.tokens--;
        return true;
    }

    private static final class Bucket {
        double tokens;
        long lastSeen;

        Bucket(double tokens, long lastSeen) {
            this.tokens = tokens;
            this.lastSeen = lastSeen;
        }
    }
}
