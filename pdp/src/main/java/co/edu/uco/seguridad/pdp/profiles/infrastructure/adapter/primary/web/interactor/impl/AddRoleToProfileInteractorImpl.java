package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.profiles.application.usecase.AddRoleToProfileUseCase;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.request.raw.AddRoleToProfileRawRequest;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.response.ProfileWebResponse;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.interactor.AddRoleToProfileInteractor;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.mapper.AddRoleToProfileRequestMapper;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.mapper.ProfileResponseMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Lee el principal (el inquilino nunca viene del cuerpo), mapea raw a request, ejecuta y aplana la respuesta. */
public final class AddRoleToProfileInteractorImpl implements AddRoleToProfileInteractor {

    private final AddRoleToProfileUseCase useCase;

    public AddRoleToProfileInteractorImpl(AddRoleToProfileUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.ADD_ROLE_TO_PROFILE_USE_CASE);
    }

    @Override
    public Mono<ProfileWebResponse> execute(AddRoleToProfileRawRequest input) {
        return SecurityContext.currentPrincipal()
                .map(principal -> AddRoleToProfileRequestMapper.toRequest(input, principal.tenantId()))
                .flatMap(useCase::execute)
                .map(ProfileResponseMapper::toResponse);
    }
}
