package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.profiles.application.usecase.DefineProfileUseCase;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.request.raw.DefineProfileRawRequest;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.response.ProfileWebResponse;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.interactor.DefineProfileInteractor;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.mapper.DefineProfileRequestMapper;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.mapper.ProfileResponseMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Lee el principal (el inquilino nunca viene del cuerpo), mapea raw a request, ejecuta y aplana la respuesta. */
public final class DefineProfileInteractorImpl implements DefineProfileInteractor {

    private final DefineProfileUseCase useCase;

    public DefineProfileInteractorImpl(DefineProfileUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.DEFINE_PROFILE_USE_CASE);
    }

    @Override
    public Mono<ProfileWebResponse> execute(DefineProfileRawRequest input) {
        return SecurityContext.currentPrincipal()
                .map(principal -> DefineProfileRequestMapper.toRequest(input, principal.tenantId()))
                .flatMap(useCase::execute)
                .map(ProfileResponseMapper::toResponse);
    }
}
