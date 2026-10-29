package co.edu.uco.seguridad.pdp.authorization.application.primaryport.response;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;

import java.util.List;

/** Vista segura para el sujeto actual: no contiene claims. */
public record CurrentUserApplicationsResponse(String tenantName, String userName, String email,
                                              List<Application> applications) {
    public CurrentUserApplicationsResponse { applications = List.copyOf(applications); }
    public record Application(ApplicationId id, String name, List<NamedRole> tenantRoles,
                              List<NamedRole> applicationRoles, List<Profile> profiles) {
        public Application { tenantRoles = List.copyOf(tenantRoles); applicationRoles = List.copyOf(applicationRoles); profiles = List.copyOf(profiles); }
    }
    public record Profile(String name, List<ProfileRole> roles) { public Profile { roles = List.copyOf(roles); } }
    public record NamedRole(String name) { }
    public record ProfileRole(String name, String scope) { }
}
