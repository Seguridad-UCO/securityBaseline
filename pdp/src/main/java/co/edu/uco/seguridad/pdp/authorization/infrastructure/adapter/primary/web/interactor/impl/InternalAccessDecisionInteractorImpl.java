package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.authorization.application.usecase.EvaluateInternalAccessUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AccessDecisionRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AccessDecisionInternalWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.InternalAccessDecisionInteractor;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Pendiente (HU-003): compara {@code requestId}/{@code correlationId} del header (vía
 * {@code CorrelationWebFilter.context(...)}, leído del contexto de Reactor — no del {@code exchange}
 * directamente, igual que {@code AuthorizeInteractorImpl}) contra el cuerpo — barreras C2/C3 del
 * plan — antes de mapear con {@code AccessDecisionRawRequestMapper}, delegar en
 * {@link EvaluateInternalAccessUseCase} y mapear la respuesta con
 * {@code AccessDecisionInternalResponseMapper}. El {@code subject} sale del {@code Jwt} de evidencia
 * ya autenticado por la cadena de {@code /internal/v1/**} — nunca de {@code SecurityContext.currentPrincipal()}
 * (T1: ese camino exige el claim {@code tenant}, que este canal no tiene).
 */
public final class InternalAccessDecisionInteractorImpl implements InternalAccessDecisionInteractor {

    private final EvaluateInternalAccessUseCase useCase;

    public InternalAccessDecisionInteractorImpl(EvaluateInternalAccessUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.EVALUATE_INTERNAL_ACCESS_USE_CASE);
    }

    @Override
    public Mono<AccessDecisionInternalWebResponse> execute(AccessDecisionRawRequest input) {
        throw new UnsupportedOperationException("pendiente: HU-003");
    }
}
