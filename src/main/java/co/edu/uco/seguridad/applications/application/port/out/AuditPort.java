package co.edu.uco.seguridad.applications.application.port.out;

import co.edu.uco.seguridad.applications.domain.ProtectedApplication;
import reactor.core.publisher.Mono;

public interface AuditPort { Mono<Void> registered(ProtectedApplication application); }
