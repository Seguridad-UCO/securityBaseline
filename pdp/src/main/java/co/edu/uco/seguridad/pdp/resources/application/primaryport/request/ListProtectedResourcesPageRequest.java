package co.edu.uco.seguridad.pdp.resources.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;

import java.util.Objects;

/**
 * Consulta paginada del catálogo de recursos de una sola aplicación.
 */
public record ListProtectedResourcesPageRequest(ApplicationId applicationId, PageWindow window, String query) {
    public ListProtectedResourcesPageRequest(ApplicationId applicationId, PageWindow window) { this(applicationId, window, ""); }
    public ListProtectedResourcesPageRequest {
        Objects.requireNonNull(applicationId);
        Objects.requireNonNull(window);
        query = query == null ? "" : query.trim();
    }
}
