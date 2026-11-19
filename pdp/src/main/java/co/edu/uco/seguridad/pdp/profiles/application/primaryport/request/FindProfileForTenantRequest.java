package co.edu.uco.seguridad.pdp.profiles.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;

import java.util.Objects;

public record FindProfileForTenantRequest(ProfileId profileId, TenantId tenantId) {
    public FindProfileForTenantRequest {
        Objects.requireNonNull(profileId);
        Objects.requireNonNull(tenantId);
    }
}
