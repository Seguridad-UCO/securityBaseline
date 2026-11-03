package co.edu.uco.seguridad.pdp.authorization.domain.event;

import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessEventTests {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID DECISION_ID = UUID.randomUUID();
    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final ResourcePath PATH = new ResourcePath("/estudiantes");
    private static final Instant OCCURRED_ON = Instant.parse("2026-09-12T00:00:00Z");

    private static AccessEvent event(UUID eventId, UUID decisionId, String requestId, String correlationId,
                                     TenantId tenantId, ApplicationId applicationId, String subject, ResourcePath resourcePath,
                                     HttpVerb action, DecisionState state, ReasonCode reasonCode, Instant occurredOn) {
        return new AccessEvent(eventId, decisionId, requestId, correlationId, tenantId, applicationId, subject,
                resourcePath, action, state, reasonCode, occurredOn);
    }

    @Test
    void rejects_a_null_event_id() {
        assertThatThrownBy(() -> event(null, DECISION_ID, "req-1", "corr-1", TENANT, APPLICATION, "subject", PATH,
                HttpVerb.GET, DecisionState.DENY, ReasonCode.NO_APPLICABLE_POLICY, OCCURRED_ON))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_a_null_decision_id() {
        assertThatThrownBy(() -> event(EVENT_ID, null, "req-1", "corr-1", TENANT, APPLICATION, "subject", PATH,
                HttpVerb.GET, DecisionState.DENY, ReasonCode.NO_APPLICABLE_POLICY, OCCURRED_ON))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_a_null_request_id() {
        assertThatThrownBy(() -> event(EVENT_ID, DECISION_ID, null, "corr-1", TENANT, APPLICATION, "subject", PATH,
                HttpVerb.GET, DecisionState.DENY, ReasonCode.NO_APPLICABLE_POLICY, OCCURRED_ON))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_a_null_correlation_id() {
        assertThatThrownBy(() -> event(EVENT_ID, DECISION_ID, "req-1", null, TENANT, APPLICATION, "subject", PATH,
                HttpVerb.GET, DecisionState.DENY, ReasonCode.NO_APPLICABLE_POLICY, OCCURRED_ON))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_a_null_tenant_id() {
        assertThatThrownBy(() -> event(EVENT_ID, DECISION_ID, "req-1", "corr-1", null, APPLICATION, "subject", PATH,
                HttpVerb.GET, DecisionState.DENY, ReasonCode.NO_APPLICABLE_POLICY, OCCURRED_ON))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_a_null_application_id() {
        assertThatThrownBy(() -> event(EVENT_ID, DECISION_ID, "req-1", "corr-1", TENANT, null, "subject", PATH,
                HttpVerb.GET, DecisionState.DENY, ReasonCode.NO_APPLICABLE_POLICY, OCCURRED_ON))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_a_null_subject() {
        assertThatThrownBy(() -> event(EVENT_ID, DECISION_ID, "req-1", "corr-1", TENANT, APPLICATION, null, PATH,
                HttpVerb.GET, DecisionState.DENY, ReasonCode.NO_APPLICABLE_POLICY, OCCURRED_ON))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_a_null_resource_path() {
        assertThatThrownBy(() -> event(EVENT_ID, DECISION_ID, "req-1", "corr-1", TENANT, APPLICATION, "subject", null,
                HttpVerb.GET, DecisionState.DENY, ReasonCode.NO_APPLICABLE_POLICY, OCCURRED_ON))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_a_null_action() {
        assertThatThrownBy(() -> event(EVENT_ID, DECISION_ID, "req-1", "corr-1", TENANT, APPLICATION, "subject", PATH,
                null, DecisionState.DENY, ReasonCode.NO_APPLICABLE_POLICY, OCCURRED_ON))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_a_null_state() {
        assertThatThrownBy(() -> event(EVENT_ID, DECISION_ID, "req-1", "corr-1", TENANT, APPLICATION, "subject", PATH,
                HttpVerb.GET, null, ReasonCode.NO_APPLICABLE_POLICY, OCCURRED_ON))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_a_null_reason_code() {
        assertThatThrownBy(() -> event(EVENT_ID, DECISION_ID, "req-1", "corr-1", TENANT, APPLICATION, "subject", PATH,
                HttpVerb.GET, DecisionState.DENY, null, OCCURRED_ON))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_a_null_occurred_on() {
        assertThatThrownBy(() -> event(EVENT_ID, DECISION_ID, "req-1", "corr-1", TENANT, APPLICATION, "subject", PATH,
                HttpVerb.GET, DecisionState.DENY, ReasonCode.NO_APPLICABLE_POLICY, null))
                .isInstanceOf(NullPointerException.class);
    }
}
