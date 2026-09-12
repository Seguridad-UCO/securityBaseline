package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AccessRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AuthorizeRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;

import java.util.Optional;
import java.util.Set;

/** raw -> AccessRequest, usando RequestFieldParser. El tenant/subject llegan ya resueltos. */
public final class AuthorizeRequestMapper {

    private AuthorizeRequestMapper() {
    }

    public static AccessRequest toRequest(AuthorizeRawRequest raw, TenantId tenantId, String subject,
            String requestId, String correlationId, Optional<UserId> subjectUserId) {
        return new AccessRequest(
                tenantId,
                subject,
                RequestFieldParser.parse("applicationId", raw.applicationId(), ApplicationId::of),
                RequestFieldParser.parse("resourcePath", raw.resourcePath(), ResourcePath::new),
                RequestFieldParser.parse("action", raw.action(), HttpVerb::parse),
                requestId,
                correlationId,
                subjectUserId,
                Set.of());
    }
}
