package co.edu.uco.seguridad.pdp.assignments.application.primaryport.request;

import co.edu.uco.seguridad.pdp.assignments.domain.AssignmentCriteria;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/** Entrada tipada de ListAssignmentsUseCase: el criterio ya validado y la ventana ya resuelta. */
public record ListAssignmentsRequest(AssignmentCriteria criteria, PageWindow window) {

    public ListAssignmentsRequest {
        Objects.requireNonNull(criteria, RequiredArgumentMessages.ASSIGNMENT_CRITERIA);
        Objects.requireNonNull(window, RequiredArgumentMessages.PAGE_WINDOW);
    }
}
