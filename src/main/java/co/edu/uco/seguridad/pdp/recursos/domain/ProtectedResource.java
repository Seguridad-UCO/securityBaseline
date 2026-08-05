package co.edu.uco.seguridad.pdp.recursos.domain;
import co.edu.uco.seguridad.pdp.commons.*;
import java.time.Instant;
public record ProtectedResource(ResourceId id, ApplicationId applicationId, TenantId tenantId, ResourceCode code, ActionCode action, Instant registeredAt) { }
