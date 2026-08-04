package co.edu.uco.seguridad.applications.domain;

import java.util.Optional;

/** Dynamic query specification. New criteria are data, not new repository methods. */
public record ProtectedApplicationCriteria(Optional<TenantId> tenantId, Optional<String> nameContains,
                                           Optional<String> resourceContains) {
    public ProtectedApplicationCriteria {
        tenantId = tenantId == null ? Optional.empty() : tenantId;
        nameContains = normalize(nameContains);
        resourceContains = normalize(resourceContains);
    }
    private static Optional<String> normalize(Optional<String> value) {
        return value == null ? Optional.empty() : value.map(String::trim).filter(s -> !s.isEmpty());
    }
    public boolean matches(ProtectedApplication app) {
        return tenantId.map(id -> id.equals(app.tenantId())).orElse(true)
            && nameContains.map(text -> app.name().value().toLowerCase().contains(text.toLowerCase())).orElse(true)
            && resourceContains.map(text -> app.resources().stream().anyMatch(r -> r.identifier().value().contains(text))).orElse(true);
    }
}
