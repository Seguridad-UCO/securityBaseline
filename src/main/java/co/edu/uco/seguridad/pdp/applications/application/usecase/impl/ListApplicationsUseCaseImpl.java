package co.edu.uco.seguridad.pdp.applications.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ListApplicationsRequest;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.application.usecase.ListApplicationsUseCase;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.commons.ResultPage;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.observability.ReactiveLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Consulta paginada del catálogo de aplicaciones de un inquilino.
 *
 * <p>No decide nada: el criterio y la ventana llegan ya validados desde el borde, y el orden lo fija
 * el adaptador. Aquí solo se delega y se proyecta el resultado al DTO de salida, conservando el
 * total y la ventana que devolvió el puerto.
 */
public final class ListApplicationsUseCaseImpl implements ListApplicationsUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ListApplicationsUseCaseImpl.class);

    private final ApplicationRepository repository;

    public ListApplicationsUseCaseImpl(ApplicationRepository repository) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.APPLICATION_REPOSITORY);
    }

    @Override
    public Mono<ResultPage<RegisteredApplicationResponse>> execute(ListApplicationsRequest dto) {
        return repository.findBy(dto.criteria(), dto.window())
                .map(page -> page.map(ListApplicationsUseCaseImpl::toRegistered))
                .transform(ReactiveLogContext.withContext(LOG, "application.list"));
    }

    private static RegisteredApplicationResponse toRegistered(Application application) {
        return new RegisteredApplicationResponse(application.id(), application.tenantId(), application.name(),
                application.description(), application.baseUrl(), application.registeredAt());
    }
}
