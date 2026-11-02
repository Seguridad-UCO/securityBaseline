package co.edu.uco.seguridad.pdp.assignments.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;

import java.util.Objects;

public record ListRoleApplicationAssignmentsPageRequest(TenantId tenantId, ApplicationId applicationId,
                                                        RoleId roleId, PageWindow window) {
    public ListRoleApplicationAssignmentsPageRequest {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(applicationId);
        Objects.requireNonNull(roleId);
        Objects.requireNonNull(window);
    }
}
