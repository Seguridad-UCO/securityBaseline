package co.edu.uco.seguridad.pdp.aplicaciones;
import co.edu.uco.seguridad.pdp.commons.TenantId;
/** Public module-level failure contract for callers allowed to depend on Aplicaciones. */
public final class DuplicateApplicationException extends RuntimeException { public DuplicateApplicationException(TenantId tenant, String name) { super("Application already exists for tenant " + tenant.value() + ": " + name); } }
