package co.edu.uco.seguridad.applications.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Aggregate root. Construction is the single integrity gate for the baseline registration. */
public final class ProtectedApplication {
    private final ProtectedApplicationId id;
    private final ApplicationName name;
    private final TenantId tenantId;
    private final List<ProtectedResource> resources;
    private final Instant registeredAt;

    private ProtectedApplication(ProtectedApplicationId id, ApplicationName name, TenantId tenantId,
                                 List<ProtectedResource> resources, Instant registeredAt) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.resources = List.copyOf(resources);
        this.registeredAt = Objects.requireNonNull(registeredAt);
        if (this.resources.size() != 1) throw new DomainException("A baseline application must contain exactly one initial resource");
    }

    public static ProtectedApplication register(ProtectedApplicationId id, ApplicationName name,
                                                TenantId tenantId, ResourceIdentifier resource, Instant now) {
        return new ProtectedApplication(id, name, tenantId, List.of(new ProtectedResource(resource)), now);
    }
    public ProtectedApplicationId id() { return id; }
    public ApplicationName name() { return name; }
    public TenantId tenantId() { return tenantId; }
    public List<ProtectedResource> resources() { return resources; }
    public Instant registeredAt() { return registeredAt; }
}
