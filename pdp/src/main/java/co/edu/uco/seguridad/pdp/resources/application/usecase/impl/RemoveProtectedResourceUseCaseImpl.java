package co.edu.uco.seguridad.pdp.resources.application.usecase.impl;

import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.application.usecase.RemoveProtectedResourceUseCase;
import co.edu.uco.seguridad.pdp.resources.domain.exception.ProtectedResourceNotFoundException;
import reactor.core.publisher.Mono;

public final class RemoveProtectedResourceUseCaseImpl implements RemoveProtectedResourceUseCase {
    private final ProtectedResourceRepository repository;

    public RemoveProtectedResourceUseCaseImpl(ProtectedResourceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Void> execute(Request input) {
        return repository.findByIdForTenant(input.resourceId(), input.tenantId())
                .switchIfEmpty(Mono.error(() -> new ProtectedResourceNotFoundException(input.resourceId())))
                .flatMap(resource -> repository.deleteById(resource.id()));
    }
}
