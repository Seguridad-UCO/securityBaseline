package co.edu.uco.seguridad.pep.commons;

import java.time.Instant;
import java.util.Objects;

/**
 * Published language v1. Identity evidence travels separately, never in this DTO.
 */
public record AccessRequest(String version, String requestId, String correlationId, Instant timestamp,
                            Application application, Resource resource, Context context) {
    public AccessRequest {
        if (!"1".equals(version)) throw new IllegalArgumentException("Unsupported contract version");
        Objects.requireNonNull(requestId);
        Objects.requireNonNull(correlationId);
        Objects.requireNonNull(timestamp);
        Objects.requireNonNull(application);
        Objects.requireNonNull(resource);
        Objects.requireNonNull(context);
    }

    public record Application(String name, String environment) {
    }

    public record Resource(String path, String action) {
    }

    public record Context(String method, String channel) {
    }
}
