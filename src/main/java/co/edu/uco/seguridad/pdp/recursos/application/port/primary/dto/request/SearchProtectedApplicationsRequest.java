package co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request;

import co.edu.uco.seguridad.crosscutting.messages.RequiredArgumentMessages;
import co.edu.uco.seguridad.pdp.commons.PageWindow;
import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedApplicationCriteria;

import java.util.Objects;

/**
 * DTO de entrada del puerto primario: búsqueda de catálogo (qué coincidir y cuánto devolver).
 *
 * <p>La ventana es obligatoria — no hay forma de expresar una consulta no acotada.</p>
 */
public record SearchProtectedApplicationsRequest(ProtectedApplicationCriteria criteria, PageWindow window) {

    public SearchProtectedApplicationsRequest {
        Objects.requireNonNull(criteria, RequiredArgumentMessages.SEARCH_CRITERIA);
        Objects.requireNonNull(window, RequiredArgumentMessages.RESULT_WINDOW);
    }
}
