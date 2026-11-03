package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;

import java.util.Objects;

public record ListApplicationSecurityRolesRequest(AdministrationRequest administration, PageWindow window, String query) {
    public ListApplicationSecurityRolesRequest(AdministrationRequest administration, PageWindow window) {
        this(administration, window, "");
    }
    public ListApplicationSecurityRolesRequest {
        Objects.requireNonNull(administration);
        Objects.requireNonNull(window);
        query = query == null ? "" : query.trim();
    }
}
