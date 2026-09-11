package co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.identity.application.usecase.ListUsersUseCase;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.dto.response.UserWebResponse;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.interactor.ListUsersInteractor;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.mapper.UserResponseMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

public final class ListUsersInteractorImpl implements ListUsersInteractor {

    private final ListUsersUseCase useCase;

    public ListUsersInteractorImpl(ListUsersUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.SEARCH_USE_CASE);
    }

    @Override
    public Mono<List<UserWebResponse>> execute() {
        return useCase.execute().map(UserResponseMapper::toResponseList);
    }
}
