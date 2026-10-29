package co.edu.uco.seguridad.pdp.applications.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import java.util.Objects;
public record ApplicationNameForTenantQuery(TenantId tenantId, ApplicationName name) { public ApplicationNameForTenantQuery { Objects.requireNonNull(tenantId); Objects.requireNonNull(name); } }
