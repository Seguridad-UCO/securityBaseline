package co.edu.uco.seguridad.pdp.tenants;
import co.edu.uco.seguridad.pdp.commons.TenantId;
/** Public module-level failure contract; infrastructure clients must not import tenant internals. */
public final class TenantUnavailableException extends RuntimeException { public TenantUnavailableException(TenantId id) { super("Tenant is unknown or inactive: " + id.value()); } }
