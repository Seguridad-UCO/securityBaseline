package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.authorization.application.usecase.EvaluateInternalAccessUseCase;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.request.ResolveExternalIdentityRequest;
import co.edu.uco.seguridad.pdp.identity.application.usecase.ResolveExternalIdentityUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AccessDecisionRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AccessDecisionInternalWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.InternalAccessDecisionInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.AccessDecisionInternalResponseMapper;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.AccessDecisionRawRequestMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.web.exception.ConflictingRequestParametersException;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Compara {@code requestId}/{@code correlationId} del header (vía el contexto de Reactor que
 * {@code CorrelationWebFilter} ya publicó — no del {@code exchange} directamente, igual que
 * {@code AuthorizeInteractorImpl}) contra el cuerpo — barreras C2/C3 del plan — antes de mapear con
 * {@code AccessDecisionRawRequestMapper}, delegar en {@link EvaluateInternalAccessUseCase} y mapear
 * la respuesta con {@code AccessDecisionInternalResponseMapper}. El {@code subject} sale del
 * {@code Jwt} de evidencia ya autenticado por la cadena de {@code /internal/v1/**} — nunca de
 * {@code SecurityContext.currentPrincipal()} (T1: ese camino exige el claim {@code tenant}, que
 * este canal no tiene).
 */
public final class InternalAccessDecisionInteractorImpl implements InternalAccessDecisionInteractor {

    private final EvaluateInternalAccessUseCase useCase;
    private final ResolveExternalIdentityUseCase identities;

    public InternalAccessDecisionInteractorImpl(EvaluateInternalAccessUseCase useCase,
            ResolveExternalIdentityUseCase identities) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.EVALUATE_INTERNAL_ACCESS_USE_CASE);
        this.identities = Objects.requireNonNull(identities);
    }

    @Override
    public Mono<AccessDecisionInternalWebResponse> execute(AccessDecisionRawRequest input) {
        return Mono.deferContextual(context -> {
            String headerRequestId = context.getOrDefault("requestId", "");
            String headerCorrelationId = context.getOrDefault("correlationId", "");
            if (!headerRequestId.equals(input.requestId())) {
                return Mono.<AccessDecisionInternalWebResponse>error(new ConflictingRequestParametersException(
                        "requestId", WebContractMessages.headerBodyMismatch("requestId")));
            }
            if (!headerCorrelationId.equals(input.correlationId())) {
                return Mono.<AccessDecisionInternalWebResponse>error(new ConflictingRequestParametersException(
                        "correlationId", WebContractMessages.headerBodyMismatch("correlationId")));
            }
            return currentEvidence()
                    .flatMap(evidence -> identities.execute(new ResolveExternalIdentityRequest(
                            evidence.issuer(), evidence.subject()))
                            .map(userId -> AccessDecisionRawRequestMapper.toRequest(input, evidence.subject(), userId)))
                    .flatMap(useCase::execute)
                    .map(AccessDecisionInternalResponseMapper::toResponse);
        });
    }

    /**
     * El sujeto sale del {@code Jwt} de evidencia ya autenticado por la cadena de
     * {@code /internal/v1/**} — nunca de {@code SecurityContext.currentPrincipal()} (T1: ese camino
     * exige el claim {@code tenant}, que este canal no tiene). Se lee directo del
     * {@code Authentication} reactivo, sin envolverlo en {@code PdpPrincipal}.
     */
    private static Mono<EvidenceIdentity> currentEvidence() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .cast(Jwt.class)
                .map(jwt -> new EvidenceIdentity(jwt.getIssuer().toString(), jwt.getSubject()));
    }

    private record EvidenceIdentity(String issuer, String subject) {
    }
}
