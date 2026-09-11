package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.applications.application.usecase.RegisterApplicationUseCase;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.RegisterApplicationRawRequest;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response.ApplicationWebResponse;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor.RegisterApplicationInteractor;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper.ApplicationResponseMapper;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper.RegisterApplicationRequestMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Mapea el payload HTTP → DTO tipado, ejecuta el caso de uso y proyecta dominio → respuesta HTTP.
 *
 * <p>El tenant no viene del payload: se lee del principal autenticado (ADR-0003) antes de mapear.</p>
 */
public final class RegisterApplicationInteractorImpl implements RegisterApplicationInteractor {

    private final RegisterApplicationUseCase useCase;

    public RegisterApplicationInteractorImpl(RegisterApplicationUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.REGISTER_USE_CASE);
    }

    @Override
    public Mono<ApplicationWebResponse> execute(RegisterApplicationRawRequest raw) {
        return SecurityContext.currentPrincipal()
                .map(principal -> RegisterApplicationRequestMapper.toRequest(raw, principal.tenantId()))
                .flatMap(useCase::execute)
                .map(ApplicationResponseMapper::toResponse);
    }
}
