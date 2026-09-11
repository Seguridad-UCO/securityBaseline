package co.edu.uco.seguridad.pep.ingress.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;

@ConfigurationProperties("pep.ingress")
public record IngressProperties(List<Route> routes, String issuer, URI jwksUri, boolean allowInsecureHttp,
                                List<String> allowedOrigins, int maxConcurrent, int requestsPerSecond, int burst,
                                int maxRateKeys, long maxBodyBytes, Duration jwksTimeout) {

    public IngressProperties {
        routes = routes == null ? List.of() : List.copyOf(routes);
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        if (issuer == null || issuer.isBlank() || jwksUri == null)
            throw new IllegalArgumentException("JWT issuer and JWKS URI required");
        validateUri(URI.create(issuer), allowInsecureHttp, false);
        validateUri(jwksUri, allowInsecureHttp, false);
        if (maxConcurrent < 1 || requestsPerSecond < 1 || burst < 1 || maxRateKeys < 1
                || maxBodyBytes < 1 || jwksTimeout == null || jwksTimeout.isNegative() || jwksTimeout.isZero()) {
            throw new IllegalArgumentException("PEP limits must be positive");
        }
        var prefixes = new HashSet<String>();
        for (Route route : routes) {
            if (route.prefix() == null || !route.prefix().matches("/[A-Za-z0-9_-]+(/[A-Za-z0-9_-]+)*")
                    || route.prefix().startsWith("/actuator") || !prefixes.add(route.prefix())) {
                throw new IllegalArgumentException("Invalid or duplicate route prefix");
            }
            validateUri(route.target(), allowInsecureHttp, true);
            if (!identifier(route.applicationId()) || !identifier(route.environment())
                    || route.audiences() == null || route.audiences().isEmpty()
                    || route.audiences().stream().anyMatch(a -> a == null || a.isBlank())) {
                throw new IllegalArgumentException("Route application, environment and audiences required");
            }
        }
        for (String a : prefixes)
            for (String b : prefixes) {
                if (!a.equals(b) && a.startsWith(b + "/"))
                    throw new IllegalArgumentException("Overlapping route prefixes");
            }
        for (String origin : allowedOrigins) {
            validateUri(URI.create(origin), allowInsecureHttp, true);
        }
    }

    private static boolean identifier(String value) {
        return value != null && value.matches("[A-Za-z0-9_.:-]{1,128}");
    }

    private static void validateUri(URI uri, boolean insecure, boolean originOnly) {
        if (uri == null || uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawQuery() != null
                || uri.getRawFragment() != null || (!"https".equals(uri.getScheme()) && !(insecure && "http".equals(uri.getScheme())))
                || (originOnly && uri.getRawPath() != null && !uri.getRawPath().isEmpty())) {
            throw new IllegalArgumentException("Expected configured HTTPS origin/URI (HTTP only with explicit development setting)");
        }
    }

    public record Route(String prefix, String applicationId, String environment, URI target,
                        List<String> audiences, boolean forwardBearer, boolean forwardCookies) {
        public Route {
            audiences = audiences == null ? List.of() : List.copyOf(audiences);
        }
    }
}
