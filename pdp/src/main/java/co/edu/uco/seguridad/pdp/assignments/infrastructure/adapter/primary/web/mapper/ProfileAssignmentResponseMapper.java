package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ProfileAssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.ProfileAssignmentWebResponse;

/** ProfileAssignmentResponse (value objects) a ProfileAssignmentWebResponse (plana). */
public final class ProfileAssignmentResponseMapper {

    private ProfileAssignmentResponseMapper() {
    }

    public static ProfileAssignmentWebResponse toResponse(ProfileAssignmentResponse response) {
        return new ProfileAssignmentWebResponse(
                response.id().value().toString(),
                response.userId().value().toString(),
                response.tenantId().value(),
                response.applicationId().value().toString(),
                response.profileId().value().toString(),
                response.generatedAssignmentIds().stream().map(AssignmentId::value).map(Object::toString).toList(),
                response.validFrom().toString(),
                response.validUntil().map(Object::toString).orElse(null));
    }
}
