package co.edu.uco.seguridad.pdp.resources.application.message;

/**
 * Catálogo de mensajes de las excepciones de la capa de aplicación del módulo {@code resources}.
 *
 * <p>Los métodos reciben {@code String} (no objetos de valor) para que este módulo OPEN no dependa
 * de tipos de dominio de módulos cerrados.</p>
 */
public final class ResourcesMessages {

    public static String duplicateProtectedResource(String method, String path) {
        return "Ya existe un recurso registrado para " + method + " " + path + " en esta aplicación";
    }

    private ResourcesMessages() {
    }
}
