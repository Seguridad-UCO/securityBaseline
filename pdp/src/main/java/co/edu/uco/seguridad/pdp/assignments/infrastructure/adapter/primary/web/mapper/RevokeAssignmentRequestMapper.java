package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.RevokeAssignmentRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;

/** raw a RevokeAssignmentRequest con RequestFieldParser (AssignmentId::of). */
public final class RevokeAssignmentRequestMapper {

    private RevokeAssignmentRequestMapper() {
    }

    public static RevokeAssignmentRequest toRequest(RevokeAssignmentRawRequest raw, TenantId tenantId) {
        AssignmentId assignmentId = RequestFieldParser.parse("assignmentId", raw.assignmentId(), AssignmentId::of);
        return new RevokeAssignmentRequest(assignmentId, tenantId);
    }
}
