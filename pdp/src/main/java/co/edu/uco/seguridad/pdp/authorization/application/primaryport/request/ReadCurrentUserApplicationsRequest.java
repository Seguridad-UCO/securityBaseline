package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;

import java.util.Objects;

public record ReadCurrentUserApplicationsRequest(UserId userId, TenantId tenantId, String userName, String email) {
    public ReadCurrentUserApplicationsRequest { Objects.requireNonNull(userId); Objects.requireNonNull(tenantId); }
}
