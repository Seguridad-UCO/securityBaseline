package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AccessDecision;
import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.PolicyReference;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AccessDecisionInternalWebResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccessDecisionInternalResponseMapperTests {

    @Test
    void flattens_state_and_reason_code_to_strings() {
        AccessDecision decision = new AccessDecision(UUID.randomUUID(), DecisionState.DENY,
                ReasonCode.TENANT_MISMATCH, List.of(), "req-1", "corr-1", Instant.parse("2026-09-11T00:00:00Z"));

        AccessDecisionInternalWebResponse response = AccessDecisionInternalResponseMapper.toResponse(decision);

        assertThat(response.decision()).isEqualTo("DENY");
        assertThat(response.reasonCode()).isEqualTo("TENANT_MISMATCH");
        assertThat(response.decisionId()).isEqualTo(decision.decisionId().toString());
        assertThat(response.requestId()).isEqualTo("req-1");
        assertThat(response.correlationId()).isEqualTo("corr-1");
    }

    @Test
    void preserves_an_empty_policy_reference_list_instead_of_null() {
        AccessDecision decision = new AccessDecision(UUID.randomUUID(), DecisionState.DENY,
                ReasonCode.NO_APPLICABLE_POLICY, List.of(), "req-1", "corr-1", Instant.parse("2026-09-11T00:00:00Z"));

        AccessDecisionInternalWebResponse response = AccessDecisionInternalResponseMapper.toResponse(decision);

        assertThat(response.policyReferences()).isNotNull().isEmpty();
    }

    @Test
    void maps_each_policy_reference_to_its_web_counterpart() {
        AccessDecision decision = new AccessDecision(UUID.randomUUID(), DecisionState.ALLOW,
                ReasonCode.POLICY_ALLOWED, List.of(new PolicyReference("application.real-system", "1.0")),
                "req-1", "corr-1", Instant.parse("2026-09-11T00:00:00Z"));

        AccessDecisionInternalWebResponse response = AccessDecisionInternalResponseMapper.toResponse(decision);

        assertThat(response.policyReferences()).hasSize(1);
        assertThat(response.policyReferences().getFirst().policyId()).isEqualTo("application.real-system");
        assertThat(response.policyReferences().getFirst().version()).isEqualTo("1.0");
    }
}
