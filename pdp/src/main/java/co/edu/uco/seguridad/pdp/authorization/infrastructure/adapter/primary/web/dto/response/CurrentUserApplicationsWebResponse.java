package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response;

import java.util.List;

public record CurrentUserApplicationsWebResponse(Tenant tenant, User user, List<Application> applications) {
    public record Tenant(String name) { }
    public record User(String name, String email) { }
    public record Application(String name, List<String> roles) { }
}
