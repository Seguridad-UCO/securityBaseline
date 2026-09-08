package co.edu.uco.seguridad.pep.ingress.infrastructure.integration;

import co.edu.uco.seguridad.pep.ingress.infrastructure.properties.IngressProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteRegistryTests {
    @TempDir Path temporaryDirectory;

    @Test void persists_and_resolves_a_registered_application() {
        var registry = registry(List.of());
        var route = registry.register("academic", "dev", URI.create("http://academic.internal:8080"), "academic-api");

        assertThat(route.prefix()).isEqualTo("/apps/academic");
        assertThat(registry.publicUrl(route)).isEqualTo(URI.create("https://security.example.edu/apps/academic"));
        assertThat(registry.resolve("/apps/academic/notes").orElseThrow().target())
                .isEqualTo(URI.create("http://academic.internal:8080"));

        var reloaded = registry(List.of());
        assertThat(reloaded.resolve("/apps/academic/notes").orElseThrow().audiences()).containsExactly("academic-api");
    }

    @Test void refuses_a_dynamic_route_that_overlaps_a_static_route() {
        var staticRoute = new IngressProperties.Route("/apps/academic", "existing", "dev",
                URI.create("http://existing.internal"), List.of("existing-api"), false, false);

        assertThatThrownBy(() -> registry(List.of(staticRoute)).register("academic", "dev",
                URI.create("http://academic.internal"), "academic-api"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("static route");
    }

    private RouteRegistry registry(List<IngressProperties.Route> staticRoutes) {
        var password = new BCryptPasswordEncoder().encode("registration-secret");
        var properties = new IntegrationProperties(true, temporaryDirectory.resolve("routes.json"),
                URI.create("https://security.example.edu"),
                List.of(new IntegrationProperties.Credential("academic", "dev", password)));
        var ingress = new IngressProperties(staticRoutes, "http://issuer.example.edu", URI.create("http://issuer.example.edu/jwks"),
                true, List.of(), 1, 1, 1, 1, 1, Duration.ofSeconds(1));
        return new RouteRegistry(properties, ingress, JsonMapper.builder().build());
    }
}
