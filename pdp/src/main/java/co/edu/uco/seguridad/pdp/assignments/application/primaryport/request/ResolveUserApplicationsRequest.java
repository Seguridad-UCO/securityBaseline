package co.edu.uco.seguridad.pdp.assignments.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;

import java.util.Objects;

public record ResolveUserApplicationsRequest(UserId userId, TenantId tenantId) {
    public ResolveUserApplicationsRequest { Objects.requireNonNull(userId); Objects.requireNonNull(tenantId); }
}
