package co.edu.uco.seguridad.pdp.aplicaciones.application.usecase.impl;

import co.edu.uco.seguridad.pdp.aplicaciones.application.usecase.RemoveApplicationUseCase;
import co.edu.uco.seguridad.pdp.aplicaciones.application.port.secondary.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.shared.observability.ReactiveLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementación de {@link RemoveApplicationUseCase}: elimina la aplicación por su identificador.
 *
 * <p>Operación compensatoria sin reglas de negocio. Recibe un {@link ApplicationId} ya tipado,
 * por lo que no hay validación de formato ni de estado: si el id está bien formado y la
 * aplicación no existe, la eliminación simplemente completa sin efecto.</p>
 */
public final class RemoveApplicationUseCaseImpl implements RemoveApplicationUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RemoveApplicationUseCaseImpl.class);

    private final ApplicationRepository repository;

    public RemoveApplicationUseCaseImpl(ApplicationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "se requiere repositorio de aplicación");
    }

    @Override
    public Mono<Void> execute(ApplicationId applicationId) {
        return repository.deleteById(applicationId)
                .transform(ReactiveLogContext.withContext(LOG, "application.remove"));
    }
}
