package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeAssignmentRequest;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada de {@code AdministerAssignmentRevocationUseCase} (HU-018): la solicitud de revocación,
 * más la administración a gatear (resuelta vía {@code AssignmentApplicationLookupValidator}). Sin
 * {@code Optional} — siempre hay una aplicación una vez la asignación se resuelve.
 */
public record AdministerAssignmentRevocationRequest(AdministrationRequest administration,
                                                    RevokeAssignmentRequest revocation) {

    public AdministerAssignmentRevocationRequest {
        Objects.requireNonNull(administration, RequiredArgumentMessages.ADMINISTRATION_REQUEST);
        Objects.requireNonNull(revocation, RequiredArgumentMessages.REVOKE_ASSIGNMENT_REQUEST);
    }
}
