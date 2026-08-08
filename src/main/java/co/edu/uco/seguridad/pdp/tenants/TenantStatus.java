package co.edu.uco.seguridad.pdp.tenants;

/**
 * Estado del ciclo de vida de un inquilino. Forma parte del API publicado del módulo para que
 * los consumidores externos puedan interpretar un {@link co.edu.uco.seguridad.pdp.tenants.application.port.primary.dto.response.TenantResponse}
 * sin depender de tipos de dominio internos del módulo.
 *
 * <p>Solo los inquilinos {@link #ACTIVE} pueden registrar aplicaciones protegidas.</p>
 */
public enum TenantStatus {

    ACTIVE,
    SUSPENDED;

    public boolean allowsRegistration() {
        return this == ACTIVE;
    }
}
