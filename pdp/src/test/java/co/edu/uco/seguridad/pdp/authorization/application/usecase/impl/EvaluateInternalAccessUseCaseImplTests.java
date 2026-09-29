package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.exception.ApplicationNotFoundException;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationCredentialHash;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.InternalAccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AccessDecision;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AuthorizeUseCase;
import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El puente resuelve el inquilino dueño de la aplicación (D3) y arma el {@code AccessRequest} que
 * ya consume {@link AuthorizeUseCase} — la regla de decisión no cambia entre canales (D1). Cuando la
 * aplicación no existe, reporta DENY/TENANT_MISMATCH — el mismo mapeo que ya usa el canal BFF
 * (plan §3), no una excepción cruda.
 */
class EvaluateInternalAccessUseCaseImplTests {

    private static final ApplicationId APPLICATION_ID = new ApplicationId(UUID.randomUUID());
    private static final ApplicationName APPLICATION_NAME = new ApplicationName("notas");
    private static final TenantId OWNER = new TenantId("universidad-uco");
    private static final Application APPLICATION = Application.register(APPLICATION_ID, OWNER, APPLICATION_NAME, "",
            new ApplicationBaseUrl("https://notas.example.test"), new ApplicationCredentialHash("hash"), Instant.EPOCH);
    private static final ResourcePath PATH = new ResourcePath("/estudiantes");
    private static final InternalAccessRequest REQUEST = new InternalAccessRequest("evidence-subject",
            Optional.of(new co.edu.uco.seguridad.pdp.commons.model.UserId(UUID.randomUUID())), APPLICATION_NAME, PATH,
            HttpVerb.GET, "req-1", "corr-1", new InternalAccessRequest.RequestFacts(Instant.EPOCH, "", HttpVerb.GET, "HTTP"));
    private static final UUID DECISION_ID = UUID.randomUUID();
    private static final Instant DECIDED_AT = Instant.parse("2026-09-06T00:00:00Z");

    @Test
    void resolves_the_owner_tenant_and_delegates_to_authorize_use_case() {
        AccessDecision expected = new AccessDecision(UUID.randomUUID(), DecisionState.DENY,
                ReasonCode.NO_APPLICABLE_POLICY, List.of(), "req-1", "corr-1", Instant.parse("2026-09-06T00:00:00Z"));
        List<AccessRequest> received = new ArrayList<>();
        EvaluateInternalAccessUseCaseImpl useCase = new EvaluateInternalAccessUseCaseImpl(
                applicationName -> Mono.just(APPLICATION),
                request -> {
                    received.add(request);
                    return Mono.just(expected);
                },
                () -> {
                    throw new AssertionError("must not generate an id: AuthorizeUseCase already returned a decision");
                },
                () -> {
                    throw new AssertionError("must not generate a time: AuthorizeUseCase already returned a decision");
                });

        StepVerifier.create(useCase.execute(REQUEST))
                .expectNext(expected)
                .verifyComplete();

        assertThat(received).hasSize(1);
        AccessRequest built = received.getFirst();
        assertThat(built.tenantId()).isEqualTo(OWNER);
        assertThat(built.subject()).isEqualTo("evidence-subject");
        assertThat(built.subjectUserId()).isEqualTo(REQUEST.subjectUserId());
        assertThat(built.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(built.resourcePath()).isEqualTo(PATH);
        assertThat(built.action()).isEqualTo(HttpVerb.GET);
        assertThat(built.requestId()).isEqualTo("req-1");
        assertThat(built.correlationId()).isEqualTo("corr-1");
    }

    @Test
    void reports_tenant_mismatch_and_never_reaches_authorize_use_case_when_the_application_does_not_exist() {
        EvaluateInternalAccessUseCaseImpl useCase = new EvaluateInternalAccessUseCaseImpl(
                applicationName -> Mono.error(new ApplicationNotFoundException(applicationName)),
                request -> {
                    throw new AssertionError("must not reach AuthorizeUseCase: the tenant never resolved");
                },
                () -> DECISION_ID, () -> DECIDED_AT);

        StepVerifier.create(useCase.execute(REQUEST))
                .assertNext(decision -> {
                    assertThat(decision.state()).isEqualTo(DecisionState.DENY);
                    assertThat(decision.reasonCode()).isEqualTo(ReasonCode.TENANT_MISMATCH);
                    assertThat(decision.decisionId()).isEqualTo(DECISION_ID);
                    assertThat(decision.decidedAt()).isEqualTo(DECIDED_AT);
                    assertThat(decision.requestId()).isEqualTo("req-1");
                    assertThat(decision.correlationId()).isEqualTo("corr-1");
                    assertThat(decision.policyReferences()).isEmpty();
                })
                .verifyComplete();
    }
}
