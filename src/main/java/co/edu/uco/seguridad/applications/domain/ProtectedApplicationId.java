package co.edu.uco.seguridad.applications.domain;

import java.util.UUID;

public record ProtectedApplicationId(UUID value) {
    public ProtectedApplicationId {
        if (value == null) throw new DomainException("Application id is required");
    }
}
