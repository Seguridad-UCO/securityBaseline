package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.assignments.application.usecase.RegisterApplicationWithFirstAdministratorUseCase;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.RegisterApplicationWithFirstAdministratorRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.ApplicationRegisteredWebResponse;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.RegisterApplicationWithFirstAdministratorInteractor;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper.ApplicationRegisteredResponseMapper;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper.RegisterApplicationWithFirstAdministratorRequestMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Pendiente de cablear a un {@code @PostMapping} (HU-015): la ruta {@code POST /api/v1/applications}
 * sigue siendo de {@code ApplicationController} (applications) hasta que el implementador la retire
 * de allí en el mismo cambio que active este interactor aquí (ver PLAN-HU-015.md árbol §8). El
 * {@code UserId} del registrador sale de {@code principal.userId()} — vacío solo fuera del perfil
 * {@code keycloak} (ver PLAN-HU-015.md ambigüedad 1); aquí se exige con {@code orElseThrow}.
 */
public final class RegisterApplicationWithFirstAdministratorInteractorImpl
        implements RegisterApplicationWithFirstAdministratorInteractor {

    private final RegisterApplicationWithFirstAdministratorUseCase useCase;

    public RegisterApplicationWithFirstAdministratorInteractorImpl(RegisterApplicationWithFirstAdministratorUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.REGISTER_APPLICATION_USE_CASE);
    }

    @Override
    public Mono<ApplicationRegisteredWebResponse> execute(RegisterApplicationWithFirstAdministratorRawRequest raw) {
        return SecurityContext.currentPrincipal()
                .map(principal -> RegisterApplicationWithFirstAdministratorRequestMapper.toRequest(raw,
                        principal.tenantId(),
                        principal.userId().orElseThrow(() -> new IllegalStateException(
                                "El principal no trae userId resuelto — solo ocurre fuera del perfil 'keycloak'"))))
                .flatMap(useCase::execute)
                .map(ApplicationRegisteredResponseMapper::toWebResponse);
    }
}
