package co.edu.uco.seguridad.applications.application.port.in;

import co.edu.uco.seguridad.applications.domain.ProtectedApplication;
import reactor.core.publisher.Mono;

public interface RegisterProtectedApplicationUseCase {
    Mono<ProtectedApplication> register(RegisterProtectedApplicationCommand command);
}
