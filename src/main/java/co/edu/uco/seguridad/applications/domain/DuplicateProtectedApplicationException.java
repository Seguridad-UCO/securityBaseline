package co.edu.uco.seguridad.applications.domain;

public final class DuplicateProtectedApplicationException extends DomainException {
    public DuplicateProtectedApplicationException(ApplicationName name, TenantId tenantId) {
        super("A protected application with name '%s' already exists for tenant '%s'".formatted(name.value(), tenantId.value()));
    }
}
