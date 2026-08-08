package co.edu.uco.seguridad.crosscutting.messages;

/**
 * Catálogo de mensajes de las excepciones de la capa de aplicación del módulo {@code tenants}.
 *
 * <p>Los métodos reciben {@code String} (no objetos de valor ni enums de módulo) para que este
 * módulo OPEN no dependa de tipos de módulos cerrados.</p>
 */
public final class TenantsMessages {

    public static String tenantNotFound(String tenantId) {
        return "El inquilino no está registrado: " + tenantId;
    }

    public static String tenantNotActive(String tenantId, String status) {
        return "El inquilino " + tenantId + " no puede registrar aplicaciones mientras está " + status;
    }

    private TenantsMessages() {
    }
}
