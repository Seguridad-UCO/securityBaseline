package co.edu.uco.seguridad.shared.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class CorrelationWebFilter implements WebFilter {
    public static final String CONTEXT_ATTRIBUTE = CorrelationWebFilter.class.getName() + ".context";
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestId = valueOrRandom(exchange.getRequest().getHeaders().getFirst("X-Request-Id"));
        String correlationId = valueOrRandom(exchange.getRequest().getHeaders().getFirst("X-Correlation-Id"));
        var context = new RequestContext(requestId, correlationId);
        exchange.getAttributes().put(CONTEXT_ATTRIBUTE, context);
        exchange.getResponse().getHeaders().set("X-Request-Id", requestId);
        exchange.getResponse().getHeaders().set("X-Correlation-Id", correlationId);
        return chain.filter(exchange).contextWrite(values -> values.put("requestId", requestId).put("correlationId", correlationId));
    }
    private String valueOrRandom(String value) { return value == null || value.isBlank() ? UUID.randomUUID().toString() : value.trim(); }
    public static RequestContext context(ServerWebExchange exchange) { return (RequestContext) exchange.getAttribute(CONTEXT_ATTRIBUTE); }
}
