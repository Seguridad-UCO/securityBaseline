package co.edu.uco.seguridad.pdp.resources.application.usecase.impl;

import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.UpdateProtectedResourceRequest;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.response.RegisteredProtectedResourceResponse;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.application.usecase.UpdateProtectedResourceUseCase;
import co.edu.uco.seguridad.pdp.resources.domain.exception.DuplicateProtectedResourceException;
import co.edu.uco.seguridad.pdp.resources.domain.exception.ProtectedResourceNotFoundException;
import reactor.core.publisher.Mono;

public final class UpdateProtectedResourceUseCaseImpl implements UpdateProtectedResourceUseCase {
    private final ProtectedResourceRepository repository;
    public UpdateProtectedResourceUseCaseImpl(ProtectedResourceRepository repository) { this.repository = repository; }
    @Override public Mono<RegisteredProtectedResourceResponse> execute(UpdateProtectedResourceRequest input) {
        return repository.findByIdForTenant(input.resourceId(), input.tenantId())
                .switchIfEmpty(Mono.error(() -> new ProtectedResourceNotFoundException(input.resourceId())))
                .flatMap(current -> repository.existsByApplicationPathAndMethod(current.applicationId(), input.path(), input.method())
                        .filter(taken -> taken && !current.isSameEndpointAs(input.path(), input.method()))
                        .flatMap(taken -> Mono.<co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource>error(
                                new DuplicateProtectedResourceException(input.path(), input.method())))
                        .switchIfEmpty(repository.update(current.withEndpoint(input.path(), input.method()))))
                .map(resource -> new RegisteredProtectedResourceResponse(resource.id(), resource.applicationId(),
                        resource.tenantId(), resource.path(), resource.method(), resource.registeredAt()));
    }
}
