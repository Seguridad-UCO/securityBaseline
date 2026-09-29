package co.edu.uco.seguridad.pdp.resources.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.application.usecase.CountApplicationProtectedResourcesUseCase;
import reactor.core.publisher.Mono;

public final class CountApplicationProtectedResourcesUseCaseImpl implements CountApplicationProtectedResourcesUseCase {
    private final ProtectedResourceRepository repository;

    public CountApplicationProtectedResourcesUseCaseImpl(ProtectedResourceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Long> execute(ApplicationId applicationId) {
        return repository.countByApplication(applicationId);
    }
}
