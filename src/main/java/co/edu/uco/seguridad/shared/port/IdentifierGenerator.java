package co.edu.uco.seguridad.shared.port;

import java.util.UUID;

/**
 * Proporciona nuevos identificadores sustitutos para que los casos de uso nunca llamen a {@code UUID.randomUUID()} directamente
 * y permanezcan deterministas bajo prueba.
 */
@FunctionalInterface
public interface IdentifierGenerator {

    UUID next();
}
