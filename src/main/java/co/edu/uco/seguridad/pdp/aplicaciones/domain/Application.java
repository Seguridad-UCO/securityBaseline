package co.edu.uco.seguridad.pdp.aplicaciones.domain;
import co.edu.uco.seguridad.pdp.commons.*;
import java.time.Instant;
public record Application(ApplicationId id, TenantId tenantId, ApplicationName name, Instant registeredAt) { }
