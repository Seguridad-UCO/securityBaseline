package co.edu.uco.seguridad.pdp.platform.application;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformAdministrationServiceTests {
    @Test
    void exposes_the_platform_contract_value_types() {
        var application = new PlatformAdministrationService.ApplicationView("app-1", "Horarios", "Gestión", "https://horarios.test", "universidad-uco", "2026-01-01T00:00:00Z");
        var resource = new PlatformAdministrationService.ResourceView("resource-1", "app-1", "/horarios", "GET", "2026-01-01T00:00:00Z");
        var tenant = new PlatformAdministrationService.TenantView("universidad-uco", "Universidad UCO", "ACTIVE");
        var user = new PlatformAdministrationService.UserView("user-1", "user@test.edu", "User", "google", "universidad-uco", "a", "b");
        LocalUserPrincipal principal = new LocalUserPrincipal("user-1", "subject", new TenantId("universidad-uco"), "user@test.edu", "User");

        assertThat(List.of(application.id(), resource.path(), tenant.code(), user.email(), principal.tenantId().value()))
                .containsExactly("app-1", "/horarios", "universidad-uco", "user@test.edu", "universidad-uco");
    }
}
