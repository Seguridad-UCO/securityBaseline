package co.edu.uco.seguridad.pdp.authorization.application.primaryport.response;

import java.util.List;

/** Vista segura para el sujeto actual: no contiene ids internos ni claims. */
public record CurrentUserApplicationsResponse(String tenantName, String userName, String email,
                                              List<ApplicationRoles> applications) {
    public CurrentUserApplicationsResponse { applications = List.copyOf(applications); }
    public record ApplicationRoles(String name, List<String> roles) {
        public ApplicationRoles { roles = List.copyOf(roles); }
    }
}
