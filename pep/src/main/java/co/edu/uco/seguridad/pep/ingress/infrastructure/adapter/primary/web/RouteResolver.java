package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.ingress.domain.CanonicalPath;
import co.edu.uco.seguridad.pep.ingress.infrastructure.integration.RouteRegistry;
import co.edu.uco.seguridad.pep.ingress.infrastructure.properties.IngressProperties;
import org.springframework.stereotype.Component;

import static co.edu.uco.seguridad.pep.commons.EnforcementFailure.Kind.UNKNOWN_ROUTE;

@Component
public final class RouteResolver {

    private final RouteRegistry routes;

    public RouteResolver(RouteRegistry routes) {
        this.routes = routes;
    }

    public IngressProperties.Route resolve(String rawPath) {
        String path = new CanonicalPath(rawPath).value();
        return routes.resolve(path).orElseThrow(() -> new EnforcementFailure(UNKNOWN_ROUTE, "ROUTE_NOT_FOUND"));
    }
}
