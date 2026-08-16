package co.edu.uco.seguridad.pdp.tenants;

/** Estado del ciclo de vida de un inquilino. Solo {@link #ACTIVE} puede registrar aplicaciones. */
public enum TenantStatus {

    ACTIVE,
    SUSPENDED;

    public boolean allowsRegistration() {
        return this == ACTIVE;
    }
}
