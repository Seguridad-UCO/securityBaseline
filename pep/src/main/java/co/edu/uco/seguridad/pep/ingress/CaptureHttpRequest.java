package co.edu.uco.seguridad.pep.ingress;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Public HTTP adapter facade. Future SDKs consume application DTOs, not this WebFlux-specific interface.
 */
public interface CaptureHttpRequest {
    Mono<CapturedAccess> execute(ServerWebExchange exchange);
}
