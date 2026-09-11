package co.edu.uco.seguridad.pep.enforcement;

import co.edu.uco.seguridad.pep.commons.ProxyTarget;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Public transport facade; only called after successful enforcement by the composition root.
 */
public interface ForwardHttpRequest {
    Mono<Void> execute(ServerWebExchange exchange, ProxyTarget target);
}

