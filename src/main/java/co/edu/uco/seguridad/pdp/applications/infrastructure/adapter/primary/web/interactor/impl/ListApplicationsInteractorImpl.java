package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.applications.application.usecase.ListApplicationsUseCase;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response.ApplicationWebResponse;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor.ListApplicationsInteractor;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper.ApplicationResponseMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

public final class ListApplicationsInteractorImpl implements ListApplicationsInteractor {

    private final ListApplicationsUseCase useCase;

    public ListApplicationsInteractorImpl(ListApplicationsUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.SEARCH_USE_CASE);
    }

    @Override
    public Mono<List<ApplicationWebResponse>> execute() {
        return SecurityContext.currentPrincipal()
                .flatMapMany(principal -> useCase.execute(principal.tenantId()))
                .map(ApplicationResponseMapper::toResponse)
                .collectList();
    }
}
