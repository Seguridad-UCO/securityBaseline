package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeProfileAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.RevokeProfileAssignmentRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;

/** raw a RevokeProfileAssignmentRequest con RequestFieldParser (ProfileAssignmentId::of). */
public final class RevokeProfileAssignmentRequestMapper {

    private RevokeProfileAssignmentRequestMapper() {
    }

    public static RevokeProfileAssignmentRequest toRequest(RevokeProfileAssignmentRawRequest raw, TenantId tenantId) {
        ProfileAssignmentId profileAssignmentId = RequestFieldParser.parse("profileAssignmentId",
                raw.profileAssignmentId(), ProfileAssignmentId::of);
        return new RevokeProfileAssignmentRequest(profileAssignmentId, tenantId);
    }
}
