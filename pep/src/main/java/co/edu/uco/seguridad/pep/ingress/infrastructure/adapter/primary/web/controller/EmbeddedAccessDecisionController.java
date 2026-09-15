package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.commons.IdentityEvidence;
import co.edu.uco.seguridad.pep.enforcement.application.port.primary.dto.request.EnforceAccessRequest;
import co.edu.uco.seguridad.pep.enforcement.application.usecase.EnforceAccessUseCase;
import co.edu.uco.seguridad.pep.ingress.application.port.secondary.ApplicationCredentialValidationPort;
import co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.dto.request.raw.EmbeddedAccessDecisionRawRequest;
import co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.secondary.http.BffSessionTokenResolver;
import co.edu.uco.seguridad.pep.normalization.application.port.primary.dto.request.NormalizeAccessRequest;
import co.edu.uco.seguridad.pep.normalization.application.usecase.NormalizeAccessUseCase;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Plano de datos para el starter embebido. Evalúa una decisión, pero nunca reenvía la petición a
 * la aplicación: hacerlo aquí crearía un bucle con el filtro instalado por el starter.
 */
@RestController
@RequestMapping("/internal/v1/embedded-access-decisions")
final class EmbeddedAccessDecisionController {
    private final ApplicationCredentialValidationPort credentials;
    private final NormalizeAccessUseCase normalize;
    private final EnforceAccessUseCase enforce;
    private final BffSessionTokenResolver bffSession;

    EmbeddedAccessDecisionController(ApplicationCredentialValidationPort credentials, NormalizeAccessUseCase normalize,
                                     EnforceAccessUseCase enforce, BffSessionTokenResolver bffSession) {
        this.credentials = credentials;
        this.normalize = normalize;
        this.enforce = enforce;
        this.bffSession = bffSession;
    }

    @PostMapping
    Mono<ResponseEntity<Void>> evaluate(@RequestBody EmbeddedAccessDecisionRawRequest body,
                                        @RequestHeader(name = "X-Application-Credential", required = false) String credential,
                                        @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                        ServerWebExchange exchange) {
        if (credential == null || credential.isBlank()) {
            return Mono.error(new EnforcementFailure(EnforcementFailure.Kind.UNAUTHENTICATED,
                    "INTEGRATION_TOKEN_INVALID"));
        }
        String requestId = exchange.getAttribute("pep.requestId");
        String correlationId = exchange.getAttribute("pep.correlationId");
        Mono<String> evidence = validBearer(authorization) ? Mono.just(authorization.substring(7)) : bffSession.resolve(exchange);
        return credentials.validate(body.applicationName(), credential)
                .then(normalize.execute(new NormalizeAccessRequest(requestId, correlationId, Instant.now(),
                        body.applicationName(), body.environment(), body.path(), body.method())))
                .flatMap(request -> evidence.flatMap(bearer -> enforce.execute(new EnforceAccessRequest(request, new IdentityEvidence(bearer)))))
                .map(decision -> ResponseEntity.noContent().header("X-Decision-Id", decision.decisionId()).build());
    }

    private static boolean validBearer(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ") && authorization.length() > 7
                && !authorization.substring(7).isBlank();
    }
}
