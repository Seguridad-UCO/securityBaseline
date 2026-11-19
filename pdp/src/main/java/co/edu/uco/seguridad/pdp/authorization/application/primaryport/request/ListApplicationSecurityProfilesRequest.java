package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;

import java.util.Objects;

public record ListApplicationSecurityProfilesRequest(AdministrationRequest administration, PageWindow window, String query) {
    public ListApplicationSecurityProfilesRequest(AdministrationRequest administration, PageWindow window) {
        this(administration, window, "");
    }
    public ListApplicationSecurityProfilesRequest {
        Objects.requireNonNull(administration);
        Objects.requireNonNull(window);
        query = query == null ? "" : query.trim();
    }
}
