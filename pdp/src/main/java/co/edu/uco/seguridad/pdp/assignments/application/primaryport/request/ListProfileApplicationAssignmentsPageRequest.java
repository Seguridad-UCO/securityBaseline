package co.edu.uco.seguridad.pdp.assignments.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;

import java.util.Objects;

public record ListProfileApplicationAssignmentsPageRequest(TenantId tenantId, ApplicationId applicationId,
                                                           ProfileId profileId, PageWindow window) {
    public ListProfileApplicationAssignmentsPageRequest {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(applicationId);
        Objects.requireNonNull(profileId);
        Objects.requireNonNull(window);
    }
}
