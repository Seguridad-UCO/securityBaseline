package co.edu.uco.seguridad.pep.ingress.infrastructure.integration;

import co.edu.uco.seguridad.pep.ingress.application.port.primary.dto.request.RegisterIntegrationRequest;
import co.edu.uco.seguridad.pep.ingress.application.port.primary.dto.response.RegisteredIntegrationResponse;
import co.edu.uco.seguridad.pep.ingress.application.port.secondary.IntegrationRegistrationPort;
import co.edu.uco.seguridad.pep.ingress.infrastructure.properties.IngressProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Single-node, file-backed registry used by the PEP before authentication and proxying.
 */
@Component
public final class RouteRegistry implements IntegrationRegistrationPort {

    private final IntegrationProperties properties;
    private final ObjectMapper mapper;
    private final List<IngressProperties.Route> staticRoutes;
    private final boolean allowInsecureHttp;
    private final AtomicReference<List<RegisteredIntegration>> dynamicRoutes;

    public RouteRegistry(IntegrationProperties properties, IngressProperties ingress, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        this.staticRoutes = ingress.routes();
        this.allowInsecureHttp = ingress.allowInsecureHttp();
        this.dynamicRoutes = new AtomicReference<>(properties.enabled() ? load() : List.of());
    }

    public Optional<IngressProperties.Route> resolve(String path) {
        return allRoutes().stream().filter(route -> path.equals(route.prefix()) || path.startsWith(route.prefix() + "/"))
                .findFirst();
    }

    public synchronized RegisteredIntegration register(String applicationId, String environment, URI backendUrl,
                                                       String audience) {
        if (!properties.enabled()) throw new IllegalStateException("Integration registration is disabled");
        if (!applicationId.matches("[A-Za-z0-9._~-]{1,128}") || !IntegrationProperties.identifier(environment)
                || audience == null || audience.isBlank() || !audience.matches("[A-Za-z0-9_.:-]{1,128}")) {
            throw new IllegalArgumentException("Invalid application integration");
        }
        validateBackend(backendUrl);
        String prefix = "/apps/" + applicationId;
        if (staticRoutes.stream().anyMatch(route -> overlaps(prefix, route.prefix()))) {
            throw new IllegalArgumentException("Integration route conflicts with a static route");
        }
        var next = new ArrayList<>(dynamicRoutes.get());
        next.removeIf(route -> route.applicationId().equals(applicationId) && route.environment().equals(environment));
        if (next.stream().anyMatch(route -> overlaps(prefix, route.prefix()))) {
            throw new IllegalArgumentException("Integration route conflicts with another application");
        }
        var registered = new RegisteredIntegration(applicationId, environment, prefix, backendUrl, audience);
        next.add(registered);
        persist(next);
        dynamicRoutes.set(List.copyOf(next));
        return registered;
    }

    @Override
    public boolean enabled() {
        return properties.enabled();
    }

    @Override
    public Mono<RegisteredIntegrationResponse> register(RegisterIntegrationRequest request) {
        return Mono.fromCallable(() -> {
                    var route = register(request.applicationId(), request.environment(), request.backendUrl(), request.audience());
                    return new RegisteredIntegrationResponse(route.applicationId(), route.environment(), route.prefix(),
                            publicUrl(route), "ACTIVE");
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public URI publicUrl(RegisteredIntegration route) {
        return URI.create(properties.publicBaseUrl().toString() + route.prefix());
    }

    private List<IngressProperties.Route> allRoutes() {
        var routes = new ArrayList<>(staticRoutes);
        for (RegisteredIntegration route : dynamicRoutes.get()) {
            routes.add(new IngressProperties.Route(route.prefix(), route.applicationId(), route.environment(),
                    route.backendUrl(), List.of(route.audience()), false, false));
        }
        return routes;
    }

    private List<RegisteredIntegration> load() {
        Path file = properties.registryFile();
        if (!Files.exists(file)) return List.of();
        try {
            var stored = mapper.readValue(Files.readAllBytes(file), RegistryFile.class);
            if (stored.version() != 1 || stored.routes() == null)
                throw new IllegalArgumentException("Unsupported registry file");
            var routes = List.copyOf(stored.routes());
            for (RegisteredIntegration route : routes) {
                if (route.applicationId() == null || !route.applicationId().matches("[A-Za-z0-9._~-]{1,128}")
                        || !IntegrationProperties.identifier(route.environment()) || route.audience() == null
                        || !route.audience().matches("[A-Za-z0-9_.:-]{1,128}")
                        || !route.prefix().equals("/apps/" + route.applicationId())) {
                    throw new IllegalArgumentException("Invalid registry route");
                }
                validateBackend(route.backendUrl());
            }
            return routes;
        } catch (IOException error) {
            throw new IllegalStateException("Cannot load integration registry", error);
        }
    }

    private void persist(List<RegisteredIntegration> routes) {
        Path file = properties.registryFile();
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            Path temporary = Files.createTempFile(parent, ".pep-integrations-", ".json");
            Files.write(temporary, mapper.writeValueAsBytes(new RegistryFile(1, routes)));
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot persist integration registry", error);
        }
    }

    private void validateBackend(URI uri) {
        if (uri == null || uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawQuery() != null
                || uri.getRawFragment() != null || uri.getRawPath() != null && !uri.getRawPath().isEmpty()
                || (!"https".equals(uri.getScheme()) && !(allowInsecureHttp && "http".equals(uri.getScheme())))) {
            throw new IllegalArgumentException("Backend must be an HTTPS origin (HTTP only in explicit development mode)");
        }
    }

    private static boolean overlaps(String one, String two) {
        return one.equals(two) || one.startsWith(two + "/") || two.startsWith(one + "/");
    }

    private record RegistryFile(int version, List<RegisteredIntegration> routes) {
    }
}
