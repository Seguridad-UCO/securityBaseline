package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.resources.application.usecase.RegisterApplicationWithInitialResourceUseCase;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.raw.RegisterApplicationWithInitialResourceRawRequest;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.response.ApplicationWithInitialResourceWebResponse;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.RegisterApplicationWithInitialResourceInteractor;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.mapper.ApplicationWithInitialResourceResponseMapper;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.mapper.RegisterApplicationWithInitialResourceRequestMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Mapea el payload HTTP → DTO tipado, ejecuta el caso de uso y proyecta dominio → respuesta HTTP.
 *
 * <p>El tenant no viene del payload: se lee del principal autenticado (ADR-0003) antes de mapear.</p>
 */
public final class RegisterApplicationWithInitialResourceInteractorImpl
        implements RegisterApplicationWithInitialResourceInteractor {

    private final RegisterApplicationWithInitialResourceUseCase useCase;

    public RegisterApplicationWithInitialResourceInteractorImpl(RegisterApplicationWithInitialResourceUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase,
                RequiredArgumentMessages.REGISTER_APPLICATION_WITH_INITIAL_RESOURCE_USE_CASE);
    }

    @Override
    public Mono<ApplicationWithInitialResourceWebResponse> execute(RegisterApplicationWithInitialResourceRawRequest raw) {
        return SecurityContext.currentPrincipal()
                .map(principal -> RegisterApplicationWithInitialResourceRequestMapper.toRequest(raw, principal.tenantId()))
                .flatMap(useCase::execute)
                .map(ApplicationWithInitialResourceResponseMapper::toResponse);
    }
}
