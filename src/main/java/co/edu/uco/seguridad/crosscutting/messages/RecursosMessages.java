package co.edu.uco.seguridad.crosscutting.messages;

/**
 * Catálogo de mensajes de las excepciones de la capa de aplicación del módulo {@code recursos}.
 *
 * <p>Los métodos reciben {@code String} (no objetos de valor) para que este módulo OPEN no dependa
 * de tipos de dominio de módulos cerrados.</p>
 */
public final class RecursosMessages {

    public static String duplicateProtectedResource(String resourceCode, String action) {
        return "El recurso " + resourceCode + " ya otorga la acción " + action
                + " para esta aplicación";
    }

    public static String resourceTenantMismatch(String requested, String owning) {
        return "El recurso fue solicitado para el inquilino " + requested
                + " pero su aplicación pertenece al inquilino " + owning;
    }

    private RecursosMessages() {
    }
}
