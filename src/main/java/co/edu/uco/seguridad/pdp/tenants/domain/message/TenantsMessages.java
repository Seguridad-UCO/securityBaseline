package co.edu.uco.seguridad.pdp.tenants.domain.message;

/**
 * Catálogo de mensajes de las excepciones de negocio del módulo {@code tenants}.
 *
 * <p>Vive en {@code domain} porque quien lo consume son las excepciones que lanzan las reglas, y las
 * reglas son dominio. Los métodos reciben {@code String} (no objetos de valor ni enums) para que el
 * catálogo no arrastre tipos de otros módulos.</p>
 */
public final class TenantsMessages {

    public static String tenantNotFound(String tenantId) {
        return "El inquilino no está registrado: " + tenantId;
    }

    public static String tenantNotActive(String tenantId, String status) {
        return "El inquilino " + tenantId + " no puede registrar aplicaciones mientras está " + status;
    }

    public static String duplicateTenant(String tenantId) {
        return "Ya existe un inquilino con el id: " + tenantId;
    }

    private TenantsMessages() {
    }
}
