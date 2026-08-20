package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.resources.application.usecase.ListProtectedResourcesUseCase;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.response.ProtectedResourceWebResponse;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.ListProtectedResourcesInteractor;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.mapper.ProtectedResourceResponseMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

public final class ListProtectedResourcesInteractorImpl implements ListProtectedResourcesInteractor {

    private final ListProtectedResourcesUseCase useCase;

    public ListProtectedResourcesInteractorImpl(ListProtectedResourcesUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.SEARCH_USE_CASE);
    }

    @Override
    public Mono<List<ProtectedResourceWebResponse>> execute(String rawApplicationId) {
        ApplicationId applicationId = RequestFieldParser.parse("applicationId", rawApplicationId, ApplicationId::of);
        return useCase.execute(applicationId).map(ProtectedResourceResponseMapper::toResponse).collectList();
    }
}
