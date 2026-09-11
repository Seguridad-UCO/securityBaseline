package co.edu.uco.seguridad.pdp.identity.domain.message;

/**
 * Catálogo de mensajes del slice de identidad. Todo texto que llegue al usuario sale de aquí:
 * un literal dentro de una excepción es un mensaje que nadie encuentra cuando hay que cambiarlo.
 */
public final class IdentityMessages {

    public static String userNotFound(String userId) {
        return "El usuario no existe: " + userId;
    }

    private IdentityMessages() {
    }
}
