package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.crosscutting.messages.RequiredArgumentMessages;
import co.edu.uco.seguridad.pdp.recursos.application.usecase.RegisterProtectedApplicationUseCase;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedApplicationRawRequest;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.response.ProtectedApplicationResponse;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.interactor.RegisterProtectedApplicationInteractor;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.mapper.ProtectedApplicationResponseMapper;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.mapper.RegisterProtectedApplicationRequestMapper;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Mapea el payload HTTP → DTO tipado, ejecuta el caso de uso y proyecta dominio → respuesta HTTP.
 *
 * <p>El tenant no viene del payload: se lee del principal autenticado (ADR-0003) antes de mapear,
 * que es el punto de extensión que ADR-0001 reservó para la seguridad real.</p>
 */
public final class RegisterProtectedApplicationInteractorImpl implements RegisterProtectedApplicationInteractor {

    private final RegisterProtectedApplicationUseCase useCase;

    public RegisterProtectedApplicationInteractorImpl(RegisterProtectedApplicationUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.REGISTER_USE_CASE);
    }

    @Override
    public Mono<ProtectedApplicationResponse> execute(RegisterProtectedApplicationRawRequest raw) {
        return SecurityContext.currentPrincipal()
                .map(principal -> RegisterProtectedApplicationRequestMapper.toRequest(raw, principal.tenantId()))
                .flatMap(useCase::execute)
                .map(ProtectedApplicationResponseMapper::toResponse);
    }
}
