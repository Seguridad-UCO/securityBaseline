package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerApplicationRemovalUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ApplicationAdministrationRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ApplicationRemovalInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.ApplicationAdministrationRequestMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class ApplicationRemovalInteractorImpl implements ApplicationRemovalInteractor {

    private final AdministerApplicationRemovalUseCase useCase;

    public ApplicationRemovalInteractorImpl(AdministerApplicationRemovalUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.REMOVE_USE_CASE);
    }

    @Override
    public Mono<Void> execute(ApplicationAdministrationRawRequest raw) {
        return SecurityContext.currentPrincipal()
                .map(principal -> ApplicationAdministrationRequestMapper.toAdministrationRequest(raw, principal))
                .flatMap(useCase::execute);
    }
}
