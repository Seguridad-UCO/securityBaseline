package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;

import java.util.Objects;

/**
 * Lectura administrativa de recursos: contexto autorizado más ventana ya validada.
 */
public record ListApplicationSecurityResourcesRequest(AdministrationRequest administration, PageWindow window, String query) {
    public ListApplicationSecurityResourcesRequest(AdministrationRequest administration, PageWindow window) { this(administration, window, ""); }
    public ListApplicationSecurityResourcesRequest {
        Objects.requireNonNull(administration);
        Objects.requireNonNull(window);
        query = query == null ? "" : query.trim();
    }
}
