package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.properties.OpaProperties;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contra el mismo sustituto HTTP embebido de OPA que {@code OpaPolicyDecisionAdapterTests}, sin
 * Docker. A diferencia de ese adaptador, el payload no lleva {@code resource}/{@code action} — no
 * está atado al contrato {@code pdp-opa/v1} (PLAN-HU-009.md, hallazgo 7).
 */
class OpaAdministrationDecisionAdapterTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final AdministrationRequest REQUEST =
            new AdministrationRequest(TENANT, APPLICATION, USER, "test-subject", Set.of("Administrador de aplicación"));

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
    void maps_an_allow_response_to_an_allow_decision() {
        fixture.respondWith(200, """
                {"result":{"effect":"ALLOW","reasonCode":"POLICY_ALLOWED","policyReferences":[],"obligations":[]}}""");

        StepVerifier.create(adapter().execute(REQUEST))
                .assertNext(decision -> {
                    assertThat(decision.state()).isEqualTo(DecisionState.ALLOW);
                    assertThat(decision.reasonCode()).isEqualTo(ReasonCode.POLICY_ALLOWED);
                })
                .verifyComplete();
    }

    @Test
    void maps_the_default_deny_response_to_a_deny_decision() {
        StepVerifier.create(adapter().execute(REQUEST))
                .assertNext(decision -> {
                    assertThat(decision.state()).isEqualTo(DecisionState.DENY);
                    assertThat(decision.reasonCode()).isEqualTo(ReasonCode.NO_APPLICABLE_POLICY);
                })
                .verifyComplete();
    }

    @Test
    void fails_when_opa_responds_with_a_server_error() {
        fixture.respondWith(500, "{}");

        StepVerifier.create(adapter().execute(REQUEST)).expectError().verify();
    }

    @Test
    void sends_the_subject_tenant_and_application_without_resource_or_action() {
        StepVerifier.create(adapter().execute(REQUEST)).assertNext(decision -> {
        }).verifyComplete();

        JsonNode sent = new ObjectMapper().readTree(fixture.lastRequestBody());
        JsonNode input = sent.path("input");
        assertThat(input.path("subject").path("id").asString()).isEqualTo("test-subject");
        assertThat(input.path("subject").path("tenantId").asString()).isEqualTo("universidad-uco");
        assertThat(input.path("subject").path("roles").valueStream().map(JsonNode::asString).toList())
                .containsExactly("Administrador de aplicación");
        assertThat(input.path("tenant").path("id").asString()).isEqualTo("universidad-uco");
        assertThat(input.path("application").path("id").asString()).isEqualTo(APPLICATION.value().toString());
        assertThat(input.path("resource").isMissingNode()).isTrue();
        assertThat(input.path("action").isMissingNode()).isTrue();
    }

    private OpaAdministrationDecisionAdapter adapter() {
        WebClient webClient = WebClient.builder().baseUrl(fixture.baseUrl()).build();
        OpaProperties properties = new OpaProperties(fixture.baseUrl(), "/v1/data/security/authorization/decision",
                "/v1/data/security/administration/decision", Duration.ofSeconds(5));
        return new OpaAdministrationDecisionAdapter(webClient, new ObjectMapper(), properties);
    }
}
