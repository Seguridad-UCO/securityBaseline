package co.edu.uco.seguridad.shared.port;

import java.time.Instant;

/**
 * Proporciona el instante actual para que los casos de uso nunca llamen a {@code Instant.now()} directamente y permanezcan
 * deterministas bajo prueba.
 */
@FunctionalInterface
public interface TimeProvider {

    Instant now();
}
