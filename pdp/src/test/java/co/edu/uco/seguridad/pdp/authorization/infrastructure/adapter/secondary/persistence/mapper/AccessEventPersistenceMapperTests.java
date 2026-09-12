package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.mapper;

import co.edu.uco.seguridad.pdp.authorization.domain.event.AccessEvent;
import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.entity.AccessEventEntity;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccessEventPersistenceMapperTests {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID DECISION_ID = UUID.randomUUID();
    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final ResourcePath PATH = new ResourcePath("/estudiantes");
    private static final Instant OCCURRED_ON = Instant.parse("2026-09-12T00:00:00Z");

    @Test
    void converts_an_entity_row_into_the_domain_event() {
        AccessEventEntity entity = new AccessEventEntity(EVENT_ID.toString(), DECISION_ID.toString(), "req-1",
                "corr-1", TENANT.value(), APPLICATION.value().toString(), "test-subject", PATH.value(),
                HttpVerb.GET.name(), DecisionState.DENY.name(), ReasonCode.NO_APPLICABLE_POLICY.name(),
                OCCURRED_ON.toString());

        AccessEvent event = AccessEventPersistenceMapper.toDomain(entity);

        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.decisionId()).isEqualTo(DECISION_ID);
        assertThat(event.requestId()).isEqualTo("req-1");
        assertThat(event.correlationId()).isEqualTo("corr-1");
        assertThat(event.tenantId()).isEqualTo(TENANT);
        assertThat(event.applicationId()).isEqualTo(APPLICATION);
        assertThat(event.subject()).isEqualTo("test-subject");
        assertThat(event.resourcePath()).isEqualTo(PATH);
        assertThat(event.action()).isEqualTo(HttpVerb.GET);
        assertThat(event.state()).isEqualTo(DecisionState.DENY);
        assertThat(event.reasonCode()).isEqualTo(ReasonCode.NO_APPLICABLE_POLICY);
        assertThat(event.occurredOn()).isEqualTo(OCCURRED_ON);
    }

    @Test
    void converts_the_domain_event_into_a_flat_entity_row() {
        AccessEvent event = new AccessEvent(EVENT_ID, DECISION_ID, "req-1", "corr-1", TENANT, APPLICATION,
                "test-subject", PATH, HttpVerb.GET, DecisionState.DENY, ReasonCode.NO_APPLICABLE_POLICY, OCCURRED_ON);

        AccessEventEntity entity = AccessEventPersistenceMapper.toEntity(event);

        assertThat(entity.id()).isEqualTo(EVENT_ID.toString());
        assertThat(entity.decisionId()).isEqualTo(DECISION_ID.toString());
        assertThat(entity.requestId()).isEqualTo("req-1");
        assertThat(entity.correlationId()).isEqualTo("corr-1");
        assertThat(entity.tenantId()).isEqualTo(TENANT.value());
        assertThat(entity.applicationId()).isEqualTo(APPLICATION.value().toString());
        assertThat(entity.subject()).isEqualTo("test-subject");
        assertThat(entity.resourcePath()).isEqualTo(PATH.value());
        assertThat(entity.action()).isEqualTo(HttpVerb.GET.name());
        assertThat(entity.state()).isEqualTo(DecisionState.DENY.name());
        assertThat(entity.reasonCode()).isEqualTo(ReasonCode.NO_APPLICABLE_POLICY.name());
        assertThat(entity.occurredOn()).isEqualTo(OCCURRED_ON.toString());
    }
}
