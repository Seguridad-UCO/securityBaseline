package co.edu.uco.seguridad.applications.domain;

import java.util.Objects;

/** Entity owned by the application aggregate. */
public record ProtectedResource(ResourceIdentifier identifier) {
    public ProtectedResource { Objects.requireNonNull(identifier, "identifier is required"); }
}
