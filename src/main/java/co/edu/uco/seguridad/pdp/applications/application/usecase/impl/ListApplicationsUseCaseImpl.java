package co.edu.uco.seguridad.pdp.applications.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.port.primary.dto.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.application.port.secondary.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.application.usecase.ListApplicationsUseCase;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Flux;

import java.util.Objects;

public final class ListApplicationsUseCaseImpl implements ListApplicationsUseCase {

    private final ApplicationRepository repository;

    public ListApplicationsUseCaseImpl(ApplicationRepository repository) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.APPLICATION_REPOSITORY);
    }

    @Override
    public Flux<RegisteredApplicationResponse> execute(TenantId tenantId) {
        return repository.findAllByTenant(tenantId).map(ListApplicationsUseCaseImpl::toRegistered);
    }

    private static RegisteredApplicationResponse toRegistered(Application application) {
        return new RegisteredApplicationResponse(application.id(), application.tenantId(), application.name(),
                application.description(), application.baseUrl(), application.registeredAt());
    }
}
