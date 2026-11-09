package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ProfileAssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.domain.Assignment;
import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignment;

final class AssignmentPageResponses {
    private AssignmentPageResponses() {
    }

    static AssignmentResponse role(Assignment assignment) {
        return new AssignmentResponse(assignment.id(), assignment.userId(), assignment.tenantId(), assignment.applicationId(),
                assignment.roleId(), assignment.validity().validFrom(), assignment.validity().validUntil());
    }

    static ProfileAssignmentResponse profile(ProfileAssignment assignment) {
        return new ProfileAssignmentResponse(assignment.id(), assignment.userId(), assignment.tenantId(), assignment.applicationId(),
                assignment.profileId(), assignment.generatedAssignmentIds(), assignment.validity().validFrom(), assignment.validity().validUntil());
    }
}
