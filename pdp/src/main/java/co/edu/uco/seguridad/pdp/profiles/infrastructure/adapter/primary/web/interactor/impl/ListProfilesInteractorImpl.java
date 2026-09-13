package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.profiles.application.usecase.ListProfilesUseCase;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.request.raw.ListProfilesRawRequest;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.response.ProfileWebResponse;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.interactor.ListProfilesInteractor;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.mapper.ListProfilesRequestMapper;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.mapper.ProfileResponseMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import co.edu.uco.seguridad.shared.web.PageResponse;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Lee el principal, resuelve la ventana con el mapper, ejecuta y proyecta ResultPage a PageResponse. */
public final class ListProfilesInteractorImpl implements ListProfilesInteractor {

    private final ListProfilesUseCase useCase;

    public ListProfilesInteractorImpl(ListProfilesUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.LIST_PROFILES_USE_CASE);
    }

    @Override
    public Mono<PageResponse<ProfileWebResponse>> execute(ListProfilesRawRequest input) {
        return SecurityContext.currentPrincipal()
                .map(principal -> ListProfilesRequestMapper.toRequest(input, principal.tenantId()))
                .flatMap(useCase::execute)
                .map(page -> new PageResponse<>(
                        page.content().stream().map(ProfileResponseMapper::toResponse).toList(),
                        page.total(),
                        page.window().page(),
                        page.window().offset(),
                        page.window().limit()));
    }
}
