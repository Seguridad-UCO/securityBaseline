package co.edu.uco.seguridad.pdp.profiles.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;

public record UpdateProfileRequest(TenantId tenantId, ProfileId profileId, ProfileName name) {
}
