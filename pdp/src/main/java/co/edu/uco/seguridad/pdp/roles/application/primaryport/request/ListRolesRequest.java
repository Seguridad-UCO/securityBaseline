package co.edu.uco.seguridad.pdp.roles.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.roles.domain.RoleCriteria;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/** Entrada tipada de ListRolesUseCase: el criterio ya validado y la ventana ya resuelta. */
public record ListRolesRequest(RoleCriteria criteria, PageWindow window) {

    public ListRolesRequest {
        Objects.requireNonNull(criteria, RequiredArgumentMessages.ROLE_CRITERIA);
        Objects.requireNonNull(window, RequiredArgumentMessages.PAGE_WINDOW);
    }
}
