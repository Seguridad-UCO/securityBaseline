package co.edu.uco.seguridad.pdp.assignments.application.primaryport.request;

import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignmentCriteria;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/** Entrada tipada de ListProfileAssignmentsUseCase: el criterio ya validado y la ventana ya resuelta. */
public record ListProfileAssignmentsRequest(ProfileAssignmentCriteria criteria, PageWindow window) {

    public ListProfileAssignmentsRequest {
        Objects.requireNonNull(criteria, RequiredArgumentMessages.PROFILE_ASSIGNMENT_CRITERIA);
        Objects.requireNonNull(window, RequiredArgumentMessages.PAGE_WINDOW);
    }
}
