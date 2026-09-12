package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AccessRequest;
import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.properties.OpaProperties;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contra un sustituto HTTP embebido de OPA, no contra OPA real — sin Docker. El adaptador no decide
 * nada: cada caso comprueba una traducción de ida o de vuelta, nunca una regla de negocio.
 */
class OpaPolicyDecisionAdapterTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final ResourcePath PATH = new ResourcePath("/estudiantes");
    private static final AccessRequest REQUEST =
            new AccessRequest(TENANT, "test-subject", APPLICATION, PATH, HttpVerb.GET, "req-1", "corr-1",
                    java.util.Optional.empty(), java.util.Set.of());
    private static final AccessRequest REQUEST_WITH_ROLES =
            new AccessRequest(TENANT, "test-subject", APPLICATION, PATH, HttpVerb.GET, "req-1", "corr-1",
                    java.util.Optional.empty(), java.util.Set.of("Coordinador académico"));
    private static final UUID DECISION_ID = UUID.randomUUID();
    private static final Instant DECIDED_AT = Instant.parse("2026-09-12T00:00:00Z");

    private OpaFixtureServer fixture;

    @BeforeEach
    void startFixture() throws Exception {
        fixture = OpaFixtureServer.start();
    }

    @AfterEach
    void stopFixture() {
        fixture.stop();
    }

    @Test
    void maps_an_allow_response_with_policy_references_to_an_allow_decision() {
        fixture.respondWith(200, """
                {"result":{"effect":"ALLOW","reasonCode":"POLICY_ALLOWED",
                "policyReferences":[{"id":"core.composition","version":"1.0"}],"obligations":[]}}""");

        StepVerifier.create(adapter().execute(REQUEST))
                .assertNext(decision -> {
                    assertThat(decision.state()).isEqualTo(DecisionState.ALLOW);
                    assertThat(decision.reasonCode()).isEqualTo(ReasonCode.POLICY_ALLOWED);
                    assertThat(decision.policyReferences()).hasSize(1);
                    assertThat(decision.policyReferences().get(0).policyId()).isEqualTo("core.composition");
                    assertThat(decision.policyReferences().get(0).version()).isEqualTo("1.0");
                    assertThat(decision.decisionId()).isEqualTo(DECISION_ID);
                    assertThat(decision.decidedAt()).isEqualTo(DECIDED_AT);
                    assertThat(decision.requestId()).isEqualTo("req-1");
                    assertThat(decision.correlationId()).isEqualTo("corr-1");
                })
                .verifyComplete();
    }

    @Test
    void maps_a_deny_response_with_no_applicable_policy_to_a_deny_decision() {
        StepVerifier.create(adapter().execute(REQUEST))
                .assertNext(decision -> {
                    assertThat(decision.state()).isEqualTo(DecisionState.DENY);
                    assertThat(decision.reasonCode()).isEqualTo(ReasonCode.NO_APPLICABLE_POLICY);
                    assertThat(decision.policyReferences()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void fails_when_opa_responds_with_a_server_error() {
        fixture.respondWith(500, "{}");

        StepVerifier.create(adapter().execute(REQUEST))
                .expectError()
                .verify();
    }

    @Test
    void fails_when_opa_is_slower_than_the_configured_timeout() {
        fixture.respondAfterDelay(500);

        StepVerifier.create(adapter(Duration.ofMillis(100)).execute(REQUEST))
                .expectError()
                .verify();
    }

    @Test
    void fails_the_mapping_when_opa_emits_a_reason_code_outside_the_closed_vocabulary() {
        fixture.respondWith(200, """
                {"result":{"effect":"DENY","reasonCode":"SOMETHING_OPA_NEVER_AGREED_TO",
                "policyReferences":[],"obligations":[]}}""");

        StepVerifier.create(adapter().execute(REQUEST))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void sends_the_input_wrapped_exactly_as_the_contract_expects() {
        StepVerifier.create(adapter().execute(REQUEST_WITH_ROLES)).assertNext(decision -> { }).verifyComplete();

        JsonNode sent = new ObjectMapper().readTree(fixture.lastRequestBody());
        JsonNode input = sent.path("input");
        assertThat(input.path("schemaVersion").asString()).isEqualTo("1.0");
        assertThat(input.path("request").path("id").asString()).isEqualTo("req-1");
        assertThat(input.path("request").path("correlationId").asString()).isEqualTo("corr-1");
        assertThat(input.path("subject").path("id").asString()).isEqualTo("test-subject");
        assertThat(input.path("subject").path("tenantId").asString()).isEqualTo("universidad-uco");
        assertThat(input.path("tenant").path("id").asString()).isEqualTo("universidad-uco");
        assertThat(input.path("application").path("id").asString()).isEqualTo(APPLICATION.value().toString());
        assertThat(input.path("resource").path("type").asString()).isNotEmpty();
        assertThat(input.path("resource").path("id").asString()).isEqualTo("/estudiantes");
        assertThat(input.path("action").asString()).isEqualTo("GET");
        assertThat(input.path("subject").path("roles").valueStream().map(JsonNode::asString).toList())
                .containsExactly("Coordinador académico");
    }

    private OpaPolicyDecisionAdapter adapter() {
        return adapter(Duration.ofSeconds(5));
    }

    private OpaPolicyDecisionAdapter adapter(Duration timeout) {
        WebClient webClient = WebClient.builder().baseUrl(fixture.baseUrl()).build();
        OpaProperties properties = new OpaProperties(fixture.baseUrl(), "/v1/data/security/authorization/decision", timeout);
        return new OpaPolicyDecisionAdapter(webClient, new ObjectMapper(), properties, () -> DECISION_ID, () -> DECIDED_AT);
    }
}
