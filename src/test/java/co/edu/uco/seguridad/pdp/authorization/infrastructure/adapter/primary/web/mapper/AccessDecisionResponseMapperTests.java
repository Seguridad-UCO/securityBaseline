package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AccessDecision;
import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AccessDecisionWebResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccessDecisionResponseMapperTests {

    @Test
    void flattens_state_and_reason_code_to_strings() {
        AccessDecision decision = new AccessDecision(UUID.randomUUID(), DecisionState.DENY,
                ReasonCode.NO_APPLICABLE_POLICY, List.of(), "req-1", "corr-1", Instant.parse("2026-09-06T00:00:00Z"));

        AccessDecisionWebResponse response = AccessDecisionResponseMapper.toResponse(decision);

        assertThat(response.state()).isEqualTo("DENY");
        assertThat(response.reasonCode()).isEqualTo("NO_APPLICABLE_POLICY");
        assertThat(response.requestId()).isEqualTo("req-1");
        assertThat(response.correlationId()).isEqualTo("corr-1");
        assertThat(response.decisionId()).isEqualTo(decision.decisionId().toString());
    }

    @Test
    void flattens_an_empty_policy_reference_list() {
        AccessDecision decision = new AccessDecision(UUID.randomUUID(), DecisionState.DENY,
                ReasonCode.NO_APPLICABLE_POLICY, List.of(), "req-1", "corr-1", Instant.parse("2026-09-06T00:00:00Z"));

        AccessDecisionWebResponse response = AccessDecisionResponseMapper.toResponse(decision);

        assertThat(response.policyReferences()).isEmpty();
    }
}
