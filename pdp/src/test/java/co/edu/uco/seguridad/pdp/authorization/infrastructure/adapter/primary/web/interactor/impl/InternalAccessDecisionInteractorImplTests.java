package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.InternalAccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AccessDecision;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.EvaluateInternalAccessUseCase;
import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AccessDecisionRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AccessDecisionRawRequest.RawApplication;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AccessDecisionRawRequest.RawContext;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AccessDecisionRawRequest.RawResource;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Barreras C2/C3 del plan: {@code requestId}/{@code correlationId} del header (contexto de Reactor,
 * el mismo que llena {@code CorrelationWebFilter}) deben coincidir con el cuerpo. El esqueleto de
 * {@link InternalAccessDecisionInteractorImpl} no discrimina todavía entre casos — cada prueba
 * confirma el rojo correcto (UnsupportedOperationException), no la excepción final que el
 * implementador tendrá que producir.
 */
class InternalAccessDecisionInteractorImplTests {

    private static final EvaluateInternalAccessUseCase NEVER_CALLED = request -> {
        throw new AssertionError("must not reach the use case: the mapper/barriers run first");
    };

    private static AccessDecisionRawRequest rawWith(String requestId, String correlationId) {
        return new AccessDecisionRawRequest("1", requestId, correlationId, "2026-09-11T00:00:00Z",
                new RawApplication(UUID.randomUUID().toString(), "prod"),
                new RawResource("/estudiantes", "GET"), new RawContext("GET", "HTTP"));
    }

    @Test
    void rejects_when_the_header_request_id_does_not_match_the_body() {
        InternalAccessDecisionInteractorImpl interactor = new InternalAccessDecisionInteractorImpl(NEVER_CALLED);

        StepVerifier.create(interactor.execute(rawWith("body-req", "corr-1"))
                        .contextWrite(Context.of("requestId", "header-req", "correlationId", "corr-1")))
                .expectError(UnsupportedOperationException.class)
                .verify();
        // Contrato final esperado por el plan (§3, C2): ConflictingRequestParametersException con
        // field() == "requestId". No se afirma todavía: el esqueleto no distingue entradas.
    }

    @Test
    void rejects_when_the_header_correlation_id_does_not_match_the_body() {
        InternalAccessDecisionInteractorImpl interactor = new InternalAccessDecisionInteractorImpl(NEVER_CALLED);

        StepVerifier.create(interactor.execute(rawWith("req-1", "body-corr"))
                        .contextWrite(Context.of("requestId", "req-1", "correlationId", "header-corr")))
                .expectError(UnsupportedOperationException.class)
                .verify();
        // Contrato final esperado (§3, C3): ConflictingRequestParametersException, field() == "correlationId".
    }

    @Test
    void delegates_to_the_bridge_use_case_when_headers_match_the_body() {
        AccessDecision decision = new AccessDecision(UUID.randomUUID(), DecisionState.DENY,
                ReasonCode.TENANT_MISMATCH, List.of(), "req-1", "corr-1", Instant.parse("2026-09-11T00:00:00Z"));
        InternalAccessDecisionInteractorImpl interactor = new InternalAccessDecisionInteractorImpl(
                request -> Mono.just(decision));

        StepVerifier.create(interactor.execute(rawWith("req-1", "corr-1"))
                        .contextWrite(Context.of("requestId", "req-1", "correlationId", "corr-1")))
                .expectError(UnsupportedOperationException.class)
                .verify();
        // Contrato final esperado: 200 con AccessDecisionInternalWebResponse mapeado de `decision`.
    }
}
