package co.edu.uco.seguridad.pdp.aplicaciones;
import co.edu.uco.seguridad.pdp.commons.TenantId;
public record RegisterApplicationCommand(TenantId tenantId, String name) { }
