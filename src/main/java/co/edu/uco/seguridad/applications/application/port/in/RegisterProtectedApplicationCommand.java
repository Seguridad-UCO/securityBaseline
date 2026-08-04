package co.edu.uco.seguridad.applications.application.port.in;

import co.edu.uco.seguridad.applications.domain.ApplicationName;
import co.edu.uco.seguridad.applications.domain.ResourceIdentifier;
import co.edu.uco.seguridad.applications.domain.TenantId;

import java.util.Objects;

public record RegisterProtectedApplicationCommand(ApplicationName name, TenantId tenantId, ResourceIdentifier resource) {
    public RegisterProtectedApplicationCommand { Objects.requireNonNull(name); Objects.requireNonNull(tenantId); Objects.requireNonNull(resource); }
}
