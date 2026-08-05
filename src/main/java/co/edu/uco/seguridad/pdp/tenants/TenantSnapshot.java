package co.edu.uco.seguridad.pdp.tenants;
import co.edu.uco.seguridad.pdp.commons.TenantId;
public record TenantSnapshot(TenantId id, boolean active) { }
