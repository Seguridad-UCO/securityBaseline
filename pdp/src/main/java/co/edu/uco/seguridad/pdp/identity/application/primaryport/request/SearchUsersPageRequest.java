package co.edu.uco.seguridad.pdp.identity.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;

import java.util.Objects;

public record SearchUsersPageRequest(TenantId tenantId, String query, PageWindow window) {
    public SearchUsersPageRequest {
        Objects.requireNonNull(tenantId);
        query = query == null ? "" : query.trim();
        Objects.requireNonNull(window);
    }
}
