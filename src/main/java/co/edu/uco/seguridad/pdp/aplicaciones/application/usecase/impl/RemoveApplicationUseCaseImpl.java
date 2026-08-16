package co.edu.uco.seguridad.pdp.aplicaciones.application.usecase.impl;

import co.edu.uco.seguridad.crosscutting.messages.RequiredArgumentMessages;
import co.edu.uco.seguridad.pdp.aplicaciones.application.usecase.RemoveApplicationUseCase;
import co.edu.uco.seguridad.pdp.aplicaciones.application.port.secondary.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.shared.observability.ReactiveLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementación de {@link RemoveApplicationUseCase}: elimina la aplicación por id. Operación
 * compensatoria sin reglas de negocio — si la aplicación no existe, completa sin efecto.
 */
public final class RemoveApplicationUseCaseImpl implements RemoveApplicationUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RemoveApplicationUseCaseImpl.class);

    private final ApplicationRepository repository;

    public RemoveApplicationUseCaseImpl(ApplicationRepository repository) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.APPLICATION_REPOSITORY);
    }

    @Override
    public Mono<Void> execute(ApplicationId applicationId) {
        return repository.deleteById(applicationId)
                .transform(ReactiveLogContext.withContext(LOG, "application.remove"));
    }
}
