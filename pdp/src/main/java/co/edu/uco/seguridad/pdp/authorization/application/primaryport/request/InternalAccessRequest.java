package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;
import java.time.Instant;

/**
 * Entrada de {@link co.edu.uco.seguridad.pdp.authorization.application.usecase.EvaluateInternalAccessUseCase}.
 * Como {@code AccessRequest}, pero sin {@code tenantId}: en el canal interno el inquilino se resuelve
 * antes, a partir de {@code applicationId} (HU-003, decisión D4 del HANDOFF).
 */
public record InternalAccessRequest(String subject, ApplicationId applicationId, ResourcePath resourcePath,
        HttpVerb action, String requestId, String correlationId, RequestFacts facts) {

    public InternalAccessRequest {
        Objects.requireNonNull(subject, RequiredArgumentMessages.SUBJECT);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(resourcePath, RequiredArgumentMessages.RESOURCE_PATH);
        Objects.requireNonNull(action, RequiredArgumentMessages.HTTP_METHOD);
        Objects.requireNonNull(requestId, RequiredArgumentMessages.REQUEST_ID);
        Objects.requireNonNull(correlationId, RequiredArgumentMessages.CORRELATION_ID);
        Objects.requireNonNull(facts);
    }

    public InternalAccessRequest(String subject, ApplicationId applicationId, ResourcePath resourcePath,
            HttpVerb action, String requestId, String correlationId) {
        this(subject, applicationId, resourcePath, action, requestId, correlationId,
                new RequestFacts(Instant.EPOCH, "", action, "HTTP"));
    }

    public record RequestFacts(Instant timestamp, String environment, HttpVerb method, String channel) {
        public RequestFacts {
            Objects.requireNonNull(timestamp);
            Objects.requireNonNull(environment);
            Objects.requireNonNull(method, RequiredArgumentMessages.HTTP_METHOD);
            Objects.requireNonNull(channel);
        }
    }
}
