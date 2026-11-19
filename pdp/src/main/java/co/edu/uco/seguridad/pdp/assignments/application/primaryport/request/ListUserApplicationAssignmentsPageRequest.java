package co.edu.uco.seguridad.pdp.assignments.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;

import java.util.Objects;

public record ListUserApplicationAssignmentsPageRequest(TenantId tenantId, ApplicationId applicationId,
                                                        UserId userId, PageWindow window) {
    public ListUserApplicationAssignmentsPageRequest {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(applicationId);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(window);
    }
}
