package co.edu.uco.seguridad.pdp.assignments.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;

import java.util.Objects;

public record ApplicationAssignmentCountsRequest(TenantId tenantId, ApplicationId applicationId) {
    public ApplicationAssignmentCountsRequest {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(applicationId);
    }
}
