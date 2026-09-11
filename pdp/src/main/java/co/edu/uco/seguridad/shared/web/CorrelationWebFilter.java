package co.edu.uco.seguridad.shared.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Primer filtro de la cadena: fija requestId/correlationId antes que cualquier otro código vea la
 * petición, tomándolos de {@code X-Request-Id}/{@code X-Correlation-Id} si el cliente los manda.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class CorrelationWebFilter implements WebFilter {

    public static final String CONTEXT_ATTRIBUTE = CorrelationWebFilter.class.getName() + ".context";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestId = valueOrRandom(exchange.getRequest().getHeaders().getFirst("X-Request-Id"));
        String correlationId = valueOrRandom(exchange.getRequest().getHeaders().getFirst("X-Correlation-Id"));
        RequestContext context = new RequestContext(requestId, correlationId);

        exchange.getAttributes().put(CONTEXT_ATTRIBUTE, context);
        exchange.getResponse().getHeaders().set("X-Request-Id", requestId);
        exchange.getResponse().getHeaders().set("X-Correlation-Id", correlationId);

        return chain.filter(exchange)
                .contextWrite(values -> values
                        .put("requestId", requestId)
                        .put("correlationId", correlationId));
    }

    /**
     * Expone el contexto que este filtro adjuntó a la solicitud, para que los adaptadores primarios
     * no necesiten leer cabeceras por su cuenta.
     */
    public static RequestContext context(ServerWebExchange exchange) {
        return (RequestContext) exchange.getAttribute(CONTEXT_ATTRIBUTE);
    }

    private static String valueOrRandom(String value) {
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value.trim();
    }
}
