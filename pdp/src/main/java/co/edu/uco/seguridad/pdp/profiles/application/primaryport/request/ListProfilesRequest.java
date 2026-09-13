package co.edu.uco.seguridad.pdp.profiles.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.profiles.domain.ProfileCriteria;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/** Entrada tipada de ListProfilesUseCase: el criterio ya validado y la ventana ya resuelta. */
public record ListProfilesRequest(ProfileCriteria criteria, PageWindow window) {

    public ListProfilesRequest {
        Objects.requireNonNull(criteria, RequiredArgumentMessages.PROFILE_CRITERIA);
        Objects.requireNonNull(window, RequiredArgumentMessages.PAGE_WINDOW);
    }
}
