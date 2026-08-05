package co.edu.uco.seguridad.pdp.aplicaciones;

import co.edu.uco.seguridad.pdp.commons.*;
import reactor.core.publisher.Mono;

/** Published language of Aplicaciones; implementation packages are intentionally hidden. */
public interface ApplicationsModuleApi {
    Mono<RegisteredApplication> register(RegisterApplicationCommand command);
    Mono<Void> remove(ApplicationId applicationId);
}
