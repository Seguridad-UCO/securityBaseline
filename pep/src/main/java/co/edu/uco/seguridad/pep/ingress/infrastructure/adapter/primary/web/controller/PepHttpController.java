package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pep.enforcement.ForwardHttpRequest;
import co.edu.uco.seguridad.pep.enforcement.application.port.primary.dto.request.EnforceAccessRequest;
import co.edu.uco.seguridad.pep.enforcement.application.usecase.EnforceAccessUseCase;
import co.edu.uco.seguridad.pep.ingress.CaptureHttpRequest;
import co.edu.uco.seguridad.pep.normalization.application.usecase.NormalizeAccessUseCase;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Adaptador primario del tráfico protegido: coordina los puertos de aplicación y el proxy HTTP.
 */
@RestController
public final class PepHttpController {
    private final CaptureHttpRequest capture;
    private final NormalizeAccessUseCase normalize;
    private final EnforceAccessUseCase enforce;
    private final ForwardHttpRequest forward;

    public PepHttpController(CaptureHttpRequest capture, NormalizeAccessUseCase normalize,
                             EnforceAccessUseCase enforce, ForwardHttpRequest forward) {
        this.capture = capture;
        this.normalize = normalize;
        this.enforce = enforce;
        this.forward = forward;
    }

    @RequestMapping("/**")
    public Mono<Void> handle(ServerWebExchange exchange) {
        return capture.execute(exchange).flatMap(input -> normalize.execute(input.metadata())
                .flatMap(request -> enforce.execute(new EnforceAccessRequest(request, input.evidence()))
                        .flatMap(decision -> {
                            exchange.getAttributes().put("pep.decisionId", decision.decisionId());
                            exchange.getResponse().getHeaders().set("X-Decision-Id", decision.decisionId());
                            return forward.execute(exchange, input.target());
                        })));
    }
}
