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
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.usecase.ResolveExternalIdentityUseCase;
import co.edu.uco.seguridad.shared.security.TestJwtSupport;
import co.edu.uco.seguridad.shared.web.exception.ConflictingRequestParametersException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Barreras C2/C3 del plan: {@code requestId}/{@code correlationId} del header (contexto de Reactor,
 * el mismo que llena {@code CorrelationWebFilter}) deben coincidir con el cuerpo. El sujeto sale del
 * {@code Jwt} de evidencia ya autenticado — {@link TestJwtSupport#withPrincipal} puebla
 * {@code ReactiveSecurityContextHolder} igual que la cadena de seguridad real, sin pasar por
 * {@code SecurityContext.currentPrincipal()} (ese camino exige el claim {@code tenant}, T1). El
 * tenant que se le pasa a {@code withPrincipal} no importa aquí — este canal no lo usa — el helper
 * simplemente lo exige porque también sirve a las pruebas del canal BFF.
 */
class InternalAccessDecisionInteractorImplTests {

    private static final String TENANT = "universidad-uco";
    private static final String SUBJECT = "evidence-subject";
    private static final UserId USER_ID = new UserId(UUID.randomUUID());

    private static final ResolveExternalIdentityUseCase KNOWN_IDENTITY = request -> Mono.just(Optional.of(USER_ID));

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
        InternalAccessDecisionInteractorImpl interactor = new InternalAccessDecisionInteractorImpl(NEVER_CALLED, KNOWN_IDENTITY);

        StepVerifier.create(interactor.execute(rawWith("body-req", "corr-1"))
                        .contextWrite(TestJwtSupport.withPrincipal(TENANT, SUBJECT))
                        .contextWrite(Context.of("requestId", "header-req", "correlationId", "corr-1")))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(ConflictingRequestParametersException.class)
                        .extracting(e -> ((ConflictingRequestParametersException) e).field())
                        .isEqualTo("requestId"))
                .verify();
    }

    @Test
    void rejects_when_the_header_correlation_id_does_not_match_the_body() {
        InternalAccessDecisionInteractorImpl interactor = new InternalAccessDecisionInteractorImpl(NEVER_CALLED, KNOWN_IDENTITY);

        StepVerifier.create(interactor.execute(rawWith("req-1", "body-corr"))
                        .contextWrite(TestJwtSupport.withPrincipal(TENANT, SUBJECT))
                        .contextWrite(Context.of("requestId", "req-1", "correlationId", "header-corr")))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(ConflictingRequestParametersException.class)
                        .extracting(e -> ((ConflictingRequestParametersException) e).field())
                        .isEqualTo("correlationId"))
                .verify();
    }

    @Test
    void delegates_to_the_bridge_use_case_when_headers_match_the_body() {
        AccessDecision decision = new AccessDecision(UUID.randomUUID(), DecisionState.DENY,
                ReasonCode.TENANT_MISMATCH, List.of(), "req-1", "corr-1", Instant.parse("2026-09-11T00:00:00Z"));
        List<InternalAccessRequest> received = new ArrayList<>();
        InternalAccessDecisionInteractorImpl interactor = new InternalAccessDecisionInteractorImpl(request -> {
            received.add(request);
            return Mono.just(decision);
        }, KNOWN_IDENTITY);

        StepVerifier.create(interactor.execute(rawWith("req-1", "corr-1"))
                        .contextWrite(TestJwtSupport.withPrincipal(TENANT, SUBJECT))
                        .contextWrite(Context.of("requestId", "req-1", "correlationId", "corr-1")))
                .assertNext(response -> {
                    assertThat(response.decision()).isEqualTo("DENY");
                    assertThat(response.reasonCode()).isEqualTo("TENANT_MISMATCH");
                    assertThat(response.requestId()).isEqualTo("req-1");
                    assertThat(response.correlationId()).isEqualTo("corr-1");
                })
                .verifyComplete();

        assertThat(received).hasSize(1);
        assertThat(received.getFirst().subject()).isEqualTo(SUBJECT);
        assertThat(received.getFirst().subjectUserId()).contains(USER_ID);
    }
}
