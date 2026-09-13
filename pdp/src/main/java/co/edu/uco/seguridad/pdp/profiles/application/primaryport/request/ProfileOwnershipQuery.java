package co.edu.uco.seguridad.pdp.profiles.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/** Entrada de ProfileRolesLookupValidator (HU-011), publicado a `assignments`. */
public record ProfileOwnershipQuery(TenantId tenantId, ProfileId profileId) {

    public ProfileOwnershipQuery {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(profileId, RequiredArgumentMessages.PROFILE_ID);
    }
}
