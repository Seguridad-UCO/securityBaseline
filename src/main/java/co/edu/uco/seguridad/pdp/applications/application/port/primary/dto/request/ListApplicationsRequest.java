package co.edu.uco.seguridad.pdp.applications.application.port.primary.dto.request;

import co.edu.uco.seguridad.pdp.applications.domain.ApplicationCriteria;
import co.edu.uco.seguridad.pdp.commons.PageWindow;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada tipada del caso de uso de consulta: el criterio ya validado y la ventana ya resuelta.
 * Cuando llega aquí, no queda nada que rechazar.
 */
public record ListApplicationsRequest(ApplicationCriteria criteria, PageWindow window) {

    public ListApplicationsRequest {
        Objects.requireNonNull(criteria, RequiredArgumentMessages.APPLICATION_CRITERIA);
        Objects.requireNonNull(window, RequiredArgumentMessages.PAGE_WINDOW);
    }
}
