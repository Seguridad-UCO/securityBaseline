package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeProfileAssignmentRequest;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada de {@code AdministerProfileAssignmentRevocationUseCase} (HU-019). Sin {@code Optional} —
 * resuelta vía {@code ProfileAssignmentApplicationLookupValidator}, mismo criterio que
 * {@code AdministerAssignmentRevocationRequest} (HU-018).
 */
public record AdministerProfileAssignmentRevocationRequest(AdministrationRequest administration, RevokeProfileAssignmentRequest revocation) {

    public AdministerProfileAssignmentRevocationRequest {
        Objects.requireNonNull(administration, RequiredArgumentMessages.ADMINISTRATION_REQUEST);
        Objects.requireNonNull(revocation, RequiredArgumentMessages.REVOKE_PROFILE_ASSIGNMENT_REQUEST);
    }
}
