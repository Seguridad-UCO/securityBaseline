package co.edu.uco.seguridad.pdp.authorization.domain.event;

import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.shared.event.DomainEvent;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * La evidencia correlacionada de una decisión de acceso (HU-007, INV-AUD-01). Se arma después de que
 * la decisión ya existe y nunca la modifica — ver PLAN-HU-007.md. Solo identificadores y el
 * resultado: nunca el token, la evidencia JWT ni el cuerpo de la petición (criterio 3).
 *
 * <p>Sin fábrica {@code of(AccessRequest, AccessDecision)} a propósito: esos dos tipos viven en
 * {@code application}, y {@code domain} no puede depender de {@code application}
 * (LayeredArchitectureTests). Quien arma el evento es {@code AuthorizeUseCaseImpl}, con el
 * constructor canónico — el mismo sitio que ya conoce ambos tipos.</p>
 */
public record AccessEvent(UUID eventId, UUID decisionId, String requestId, String correlationId,
        TenantId tenantId, ApplicationId applicationId, String subject, ResourcePath resourcePath,
        HttpVerb action, DecisionState state, ReasonCode reasonCode, Instant occurredOn) implements DomainEvent {

    public AccessEvent {
        Objects.requireNonNull(eventId, RequiredArgumentMessages.EVENT_ID);
        Objects.requireNonNull(decisionId, RequiredArgumentMessages.DECISION_ID);
        Objects.requireNonNull(requestId, RequiredArgumentMessages.REQUEST_ID);
        Objects.requireNonNull(correlationId, RequiredArgumentMessages.CORRELATION_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(subject, RequiredArgumentMessages.SUBJECT);
        Objects.requireNonNull(resourcePath, RequiredArgumentMessages.RESOURCE_PATH);
        Objects.requireNonNull(action, RequiredArgumentMessages.HTTP_METHOD);
        Objects.requireNonNull(state, RequiredArgumentMessages.DECISION_STATE);
        Objects.requireNonNull(reasonCode, RequiredArgumentMessages.REASON_CODE);
        Objects.requireNonNull(occurredOn, RequiredArgumentMessages.EVENT_OCCURRED_ON);
    }
}
