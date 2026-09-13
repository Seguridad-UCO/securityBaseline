package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.InternalAccessRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AccessDecisionRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * {@code AccessDecisionRawRequest} (Strings desnudos, según {@code request.schema.json}) →
 * {@link InternalAccessRequest} (value objects). Aplica las barreras C1/C4 de la sección 3 del plan
 * (versión y timestamp) — C2/C3 (coherencia de IDs con los headers) las aplica el interactor, que es
 * quien tiene el {@code RequestContext} de {@code CorrelationWebFilter}.
 */
public final class AccessDecisionRawRequestMapper {

    private AccessDecisionRawRequestMapper() {
    }

    public static InternalAccessRequest toRequest(AccessDecisionRawRequest raw, String subject) {
        String version = RequestFieldParser.requirePresent("version", raw.version());
        if (!"1".equals(version)) {
            throw new MalformedRequestFieldException("version", WebContractMessages.mustBeVersion1());
        }

        ApplicationId applicationId = RequestFieldParser.parse("applicationId", raw.application().id(), ApplicationId::of);
        ResourcePath resourcePath = RequestFieldParser.parse("resourcePath", raw.resource().path(), ResourcePath::new);
        HttpVerb action = RequestFieldParser.parse("action", raw.resource().action(), HttpVerb::parse);

        String timestamp = RequestFieldParser.requirePresent("timestamp", raw.timestamp());
        try {
            Instant.parse(timestamp);
        } catch (DateTimeParseException cause) {
            throw new MalformedRequestFieldException("timestamp", WebContractMessages.mustBeIso8601());
        }

        return new InternalAccessRequest(subject, applicationId, resourcePath, action, raw.requestId(), raw.correlationId(),
                new InternalAccessRequest.RequestFacts(Instant.parse(timestamp), raw.application().environment(),
                        RequestFieldParser.parse("context.method", raw.context().method(), HttpVerb::parse), raw.context().channel()));
    }
}
