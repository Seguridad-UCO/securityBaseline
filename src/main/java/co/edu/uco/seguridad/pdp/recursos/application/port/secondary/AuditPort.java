package co.edu.uco.seguridad.pdp.recursos.application.port.secondary;

import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedResource;
import reactor.core.publisher.Mono;

/**
 * Puerto secundario para evidencia de auditoría. La aplicación pide un registro sin saber dónde se
 * guarda; {@code Mono<Void>} porque el llamador necesita finalización, no un valor.
 */
public interface AuditPort {

    Mono<Void> protectedApplicationRegistered(ProtectedResource resource);
}
