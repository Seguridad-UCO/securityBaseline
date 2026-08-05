package co.edu.uco.seguridad.pdp.aplicaciones;
import co.edu.uco.seguridad.pdp.commons.*;
import java.time.Instant;
public record RegisteredApplication(ApplicationId id, TenantId tenantId, String name, Instant registeredAt) { }
