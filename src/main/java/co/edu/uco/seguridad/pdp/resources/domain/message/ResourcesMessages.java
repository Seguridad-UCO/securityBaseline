package co.edu.uco.seguridad.pdp.resources.domain.message;

/**
 * Catálogo de mensajes de las excepciones de negocio del módulo {@code resources}.
 *
 * <p>Los métodos reciben {@code String} (no objetos de valor) para que el catálogo no arrastre
 * tipos de otros módulos.</p>
 */
public final class ResourcesMessages {

    public static String duplicateProtectedResource(String method, String path) {
        return "Ya existe un recurso registrado para " + method + " " + path + " en esta aplicación";
    }

    private ResourcesMessages() {
    }
}
