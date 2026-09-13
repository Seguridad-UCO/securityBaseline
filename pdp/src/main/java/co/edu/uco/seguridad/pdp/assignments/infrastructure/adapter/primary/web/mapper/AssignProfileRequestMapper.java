package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignProfileRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.AssignProfileRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;

/** raw a AssignProfileRequest con RequestFieldParser (ProfileId::of, UserId::of, ApplicationId::of). */
public final class AssignProfileRequestMapper {

    private AssignProfileRequestMapper() {
    }

    public static AssignProfileRequest toRequest(AssignProfileRawRequest raw, TenantId tenantId) {
        ProfileId profileId = RequestFieldParser.parse("profileId", raw.profileId(), ProfileId::of);
        UserId userId = RequestFieldParser.parse("userId", raw.userId(), UserId::of);
        ApplicationId applicationId = RequestFieldParser.parse("applicationId", raw.applicationId(), ApplicationId::of);
        return new AssignProfileRequest(tenantId, userId, applicationId, profileId);
    }
}
