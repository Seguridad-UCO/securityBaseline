package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerUserListingUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.UserWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerUserListingInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.UserResponseMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

public final class AdministerUserListingInteractorImpl implements AdministerUserListingInteractor {

    private final AdministerUserListingUseCase useCase;
    private final TenantActorResolver actors;

    public AdministerUserListingInteractorImpl(AdministerUserListingUseCase useCase, TenantActorResolver actors) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.ADMINISTER_USER_LISTING_USE_CASE);
        this.actors = Objects.requireNonNull(actors, RequiredArgumentMessages.SUBJECT_USER_ID_LOOKUP_VALIDATOR);
    }

    @Override
    public Mono<List<UserWebResponse>> execute() {
        return SecurityContext.currentPrincipal()
                .flatMap(actors::resolve)
                .flatMap(useCase::execute)
                .map(UserResponseMapper::toResponseList);
    }
}
