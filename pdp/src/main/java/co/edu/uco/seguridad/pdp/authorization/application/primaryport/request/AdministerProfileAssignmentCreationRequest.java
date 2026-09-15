package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignProfileRequest;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada de {@code AdministerProfileAssignmentCreationUseCase} (HU-019). Sin {@code Optional} —
 * {@code AssignProfileRequest.applicationId()} siempre está presente, mismo criterio que
 * {@code AdministerAssignmentCreationRequest} (HU-018).
 */
public record AdministerProfileAssignmentCreationRequest(AdministrationRequest administration, AssignProfileRequest assignment) {

    public AdministerProfileAssignmentCreationRequest {
        Objects.requireNonNull(administration, RequiredArgumentMessages.ADMINISTRATION_REQUEST);
        Objects.requireNonNull(assignment, RequiredArgumentMessages.ASSIGN_PROFILE_REQUEST);
    }
}
