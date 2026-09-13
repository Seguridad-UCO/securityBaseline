package co.edu.uco.seguridad.pdp.assignments.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import java.util.Objects;

public record ResolveAuthorizationSubjectFactsRequest(UserId userId, TenantId tenantId, ApplicationId applicationId) {
    public ResolveAuthorizationSubjectFactsRequest { Objects.requireNonNull(userId); Objects.requireNonNull(tenantId); Objects.requireNonNull(applicationId); }
}
