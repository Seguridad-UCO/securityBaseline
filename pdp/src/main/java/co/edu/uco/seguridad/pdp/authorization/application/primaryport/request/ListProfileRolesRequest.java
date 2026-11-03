package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;

import java.util.Objects;

/**
 * Application-scoped, authorized window of the roles included in one profile.
 */
public record ListProfileRolesRequest(AdministrationRequest administration, ProfileId profileId, PageWindow window, String query) {
    public ListProfileRolesRequest(AdministrationRequest administration, ProfileId profileId, PageWindow window) {
        this(administration, profileId, window, "");
    }
    public ListProfileRolesRequest {
        Objects.requireNonNull(administration);
        Objects.requireNonNull(profileId);
        Objects.requireNonNull(window);
        query = query == null ? "" : query.trim();
    }
}
