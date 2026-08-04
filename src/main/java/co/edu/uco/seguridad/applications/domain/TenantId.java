package co.edu.uco.seguridad.applications.domain;

import java.util.Objects;

public record TenantId(String value) {
    public TenantId {
        if (value == null || value.isBlank()) throw new DomainException("Tenant id is required");
        value = value.trim();
        if (!value.matches("[a-zA-Z0-9][a-zA-Z0-9_-]{1,63}")) {
            throw new DomainException("Tenant id has an invalid format");
        }
    }
}
