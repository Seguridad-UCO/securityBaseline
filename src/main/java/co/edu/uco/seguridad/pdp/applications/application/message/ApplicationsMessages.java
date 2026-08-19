package co.edu.uco.seguridad.pdp.applications.application.message;

/**
 * Catálogo de mensajes de las excepciones de la capa de aplicación del módulo {@code applications}.
 *
 * <p>Los métodos reciben {@code String} (no objetos de valor) para que este módulo OPEN no dependa
 * de tipos de dominio de módulos cerrados.</p>
 */
public final class ApplicationsMessages {

    public static String duplicateApplication(String tenantId, String name) {
        return "El inquilino " + tenantId + " ya registró una aplicación llamada " + name;
    }

    public static String reservedApplicationName(String name) {
        return "El nombre de la aplicación está reservado por la plataforma y no se puede registrar: "
                + name;
    }

    private ApplicationsMessages() {
    }
}
