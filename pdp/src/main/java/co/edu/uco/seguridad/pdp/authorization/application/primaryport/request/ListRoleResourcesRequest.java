package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import java.util.Objects;

/** Application-scoped, authorized window of the resources granted to one role. */
public record ListRoleResourcesRequest(AdministrationRequest administration, RoleId roleId, PageWindow window) {
    public ListRoleResourcesRequest { Objects.requireNonNull(administration); Objects.requireNonNull(roleId); Objects.requireNonNull(window); }
}
