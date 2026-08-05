package co.edu.uco.seguridad.pdp.recursos;
import co.edu.uco.seguridad.pdp.commons.*;
import java.time.Instant;
public record ProtectedApplicationCatalogEntry(ApplicationId applicationId, ResourceId resourceId, TenantId tenantId, String applicationName, String resourceCode, String action, Instant registeredAt) { }
