package co.edu.uco.seguridad.pdp.applications.domain.message;

/**
 * Catálogo de mensajes de las excepciones de negocio del módulo {@code applications}.
 *
 * <p>Los métodos reciben {@code String} (no objetos de valor) para que el catálogo no arrastre
 * tipos de otros módulos.</p>
 */
public final class ApplicationsMessages {

    public static String duplicateApplication(String tenantId, String name) {
        return "El inquilino " + tenantId + " ya registró una aplicación llamada " + name;
    }

    public static String reservedApplicationName(String name) {
        return "El nombre de la aplicación está reservado por la plataforma y no se puede registrar: "
                + name;
    }

    public static String applicationNotFound(String applicationId) {
        return "La aplicación no existe en tu inquilino: " + applicationId;
    }

    private ApplicationsMessages() {
    }
}
