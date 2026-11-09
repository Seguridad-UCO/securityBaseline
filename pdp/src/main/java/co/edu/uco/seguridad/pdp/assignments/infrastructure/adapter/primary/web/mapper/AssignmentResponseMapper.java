package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.AssignmentWebResponse;

/**
 * AssignmentResponse (value objects) a AssignmentWebResponse (plana).
 */
public final class AssignmentResponseMapper {

    private AssignmentResponseMapper() {
    }

    public static AssignmentWebResponse toResponse(AssignmentResponse response) {
        return new AssignmentWebResponse(
                response.id().value().toString(),
                response.userId().value().toString(),
                response.tenantId().value(),
                response.applicationId().value().toString(),
                response.roleId().value().toString(),
                response.validFrom().toString(),
                response.validUntil().map(Object::toString).orElse(null));
    }
}
