package co.edu.uco.seguridad.pdp.applications.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.UpdateApplicationRequest;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.application.usecase.UpdateApplicationUseCase;
import co.edu.uco.seguridad.pdp.applications.domain.exception.ApplicationNotFoundException;
import co.edu.uco.seguridad.pdp.applications.domain.exception.DuplicateApplicationException;
import reactor.core.publisher.Mono;

/** Actualiza únicamente el catálogo y preserva la credencial emitida. */
public final class UpdateApplicationUseCaseImpl implements UpdateApplicationUseCase {
    private final ApplicationRepository repository;
    public UpdateApplicationUseCaseImpl(ApplicationRepository repository) { this.repository = repository; }
    @Override public Mono<RegisteredApplicationResponse> execute(UpdateApplicationRequest input) {
        return repository.findByIdForTenant(input.tenantId(), input.applicationId())
                .switchIfEmpty(Mono.error(() -> new ApplicationNotFoundException(input.applicationId())))
                .flatMap(current -> repository.existsByTenantAndName(input.tenantId(), input.name())
                        .filter(taken -> taken && !current.name().equals(input.name()))
                        .flatMap(taken -> Mono.<co.edu.uco.seguridad.pdp.applications.domain.Application>error(
                                new DuplicateApplicationException(input.tenantId(), input.name())))
                        .switchIfEmpty(repository.update(current.withDetails(input.name(), input.description(), input.baseUrl()))))
                .map(application -> new RegisteredApplicationResponse(application.id(), application.tenantId(),
                        application.name(), application.description(), application.baseUrl(), application.registeredAt()));
    }
}
