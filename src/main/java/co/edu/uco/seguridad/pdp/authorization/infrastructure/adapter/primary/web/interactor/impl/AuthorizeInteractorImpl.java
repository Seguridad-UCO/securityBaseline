package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.authorization.application.usecase.AuthorizeUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AuthorizeRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AccessDecisionWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AuthorizeInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.AccessDecisionResponseMapper;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.AuthorizeRequestMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Lee el principal, mapea raw -> AccessRequest, ejecuta el caso de uso, mapea decision -> web.
 *
 * <p>El {@code correlationId} no llega como parametro (la firma de {@link AuthorizeInteractor} solo
 * recibe el raw request): se lee del Contexto de Reactor con {@code Mono.deferContextual}, el mismo
 * mecanismo que ya usa {@code ReactiveLogContext} — {@link co.edu.uco.seguridad.shared.web.CorrelationWebFilter}
 * lo publica ahi con {@code contextWrite} antes de que la cadena llegue hasta aqui.</p>
 */
public final class AuthorizeInteractorImpl implements AuthorizeInteractor {

    private final AuthorizeUseCase useCase;

    public AuthorizeInteractorImpl(AuthorizeUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.AUTHORIZE_USE_CASE);
    }

    @Override
    public Mono<AccessDecisionWebResponse> execute(AuthorizeRawRequest input) {
        return Mono.deferContextual(context -> SecurityContext.currentPrincipal()
                        .map(principal -> AuthorizeRequestMapper.toRequest(input, principal.tenantId(),
                                principal.subject(), context.getOrDefault("correlationId", ""))))
                .flatMap(useCase::execute)
                .map(AccessDecisionResponseMapper::toResponse);
    }
}
