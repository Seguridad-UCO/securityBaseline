package co.edu.uco.seguridad.pdp.commons;

import java.util.Objects;

/** Shared-kernel identifier: no module behaviour or infrastructure. */
public record TenantId(String value) {
    public TenantId { Objects.requireNonNull(value, "tenant id is required"); value = value.trim(); if (value.isEmpty()) throw new IllegalArgumentException("tenant id is required"); }
}
