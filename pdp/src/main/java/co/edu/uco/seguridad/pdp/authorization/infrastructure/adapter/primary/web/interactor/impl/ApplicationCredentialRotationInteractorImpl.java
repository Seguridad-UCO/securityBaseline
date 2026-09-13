package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerApplicationCredentialRotationUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ApplicationAdministrationRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AdministeredApplicationWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ApplicationCredentialRotationInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.AdministeredApplicationResponseMapper;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.ApplicationAdministrationRequestMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Pendiente de cablear a un {@code @PostMapping} (HU-015): la ruta
 * {@code POST /api/v1/applications/{applicationId}/credential-rotations} sigue siendo de
 * {@code ApplicationController} (applications) hasta que el implementador la retire de allí en el
 * mismo cambio que active este interactor aquí — hacerlo antes duplicaría el mapeo HTTP y el
 * contexto de Spring no arrancaría (ver PLAN-HU-015.md árbol §8).
 */
public final class ApplicationCredentialRotationInteractorImpl implements ApplicationCredentialRotationInteractor {

    private final AdministerApplicationCredentialRotationUseCase useCase;

    public ApplicationCredentialRotationInteractorImpl(AdministerApplicationCredentialRotationUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.ROTATE_APPLICATION_CREDENTIAL_USE_CASE);
    }

    @Override
    public Mono<AdministeredApplicationWebResponse> execute(ApplicationAdministrationRawRequest raw) {
        return SecurityContext.currentPrincipal()
                .map(principal -> ApplicationAdministrationRequestMapper.toAdministrationRequest(raw, principal))
                .flatMap(useCase::execute)
                .map(AdministeredApplicationResponseMapper::toWebResponse);
    }
}
