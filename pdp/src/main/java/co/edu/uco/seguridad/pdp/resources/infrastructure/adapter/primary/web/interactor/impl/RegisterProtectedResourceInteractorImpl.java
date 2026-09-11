package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.resources.application.usecase.RegisterProtectedResourceUseCase;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedResourceRawRequest;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.response.ProtectedResourceWebResponse;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.RegisterProtectedResourceInteractor;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.mapper.ProtectedResourceResponseMapper;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.mapper.RegisterProtectedResourceRequestMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Mapea el payload HTTP → DTO tipado, ejecuta el caso de uso y proyecta dominio → respuesta HTTP.
 *
 * <p>El tenant no viene del payload: se lee del principal autenticado (ADR-0003) antes de mapear.</p>
 */
public final class RegisterProtectedResourceInteractorImpl implements RegisterProtectedResourceInteractor {

    private final RegisterProtectedResourceUseCase useCase;

    public RegisterProtectedResourceInteractorImpl(RegisterProtectedResourceUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.REGISTER_USE_CASE);
    }

    @Override
    public Mono<ProtectedResourceWebResponse> execute(RegisterProtectedResourceRawRequest raw) {
        return SecurityContext.currentPrincipal()
                .map(principal -> RegisterProtectedResourceRequestMapper.toRequest(raw, principal.tenantId()))
                .flatMap(useCase::execute)
                .map(ProtectedResourceResponseMapper::toResponse);
    }
}
