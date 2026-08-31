package co.edu.uco.seguridad.pdp.resources.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.response.RegisteredProtectedResourceResponse;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.application.usecase.ListProtectedResourcesUseCase;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Flux;

import java.util.Objects;

public final class ListProtectedResourcesUseCaseImpl implements ListProtectedResourcesUseCase {

    private final ProtectedResourceRepository repository;

    public ListProtectedResourcesUseCaseImpl(ProtectedResourceRepository repository) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.PROTECTED_RESOURCE_REPOSITORY);
    }

    @Override
    public Flux<RegisteredProtectedResourceResponse> execute(ApplicationId applicationId) {
        return repository.findAllByApplication(applicationId).map(ListProtectedResourcesUseCaseImpl::toRegistered);
    }

    private static RegisteredProtectedResourceResponse toRegistered(ProtectedResource resource) {
        return new RegisteredProtectedResourceResponse(resource.id(), resource.applicationId(), resource.tenantId(),
                resource.path(), resource.method(), resource.registeredAt());
    }
}
