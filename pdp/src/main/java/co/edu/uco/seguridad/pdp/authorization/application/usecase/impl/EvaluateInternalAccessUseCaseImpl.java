package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationNameLookupValidator;
import co.edu.uco.seguridad.pdp.applications.domain.exception.AmbiguousApplicationNameException;
import co.edu.uco.seguridad.pdp.applications.domain.exception.ApplicationNotFoundException;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.InternalAccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AccessDecision;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AuthorizeUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.EvaluateInternalAccessUseCase;
import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * {@code IdentifierGenerator}/{@code TimeProvider} no estaban en la firma que dejó el plan (FASE 5
 * del planificador) pero la sección 3 del PLAN-HU-003 sí exige construir un {@code AccessDecision}
 * DENY/TENANT_MISMATCH cuando la aplicación no existe — igual que hace {@code AuthorizeUseCaseImpl}
 * para el mismo caso. Sin ellos no hay con qué construir esa decisión. Corregido en FASE 1 del
 * tester: es completar una firma incompleta, no renegociar el contrato.
 */
public final class EvaluateInternalAccessUseCaseImpl implements EvaluateInternalAccessUseCase {

    private final ApplicationNameLookupValidator applicationLookup;
    private final AuthorizeUseCase authorizeUseCase;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public EvaluateInternalAccessUseCaseImpl(ApplicationNameLookupValidator applicationLookup,
            AuthorizeUseCase authorizeUseCase, IdentifierGenerator identifiers, TimeProvider time) {
        this.applicationLookup = Objects.requireNonNull(applicationLookup, RequiredArgumentMessages.APPLICATION_OWNER_LOOKUP_VALIDATOR);
        this.authorizeUseCase = Objects.requireNonNull(authorizeUseCase, RequiredArgumentMessages.AUTHORIZE_USE_CASE);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<AccessDecision> execute(InternalAccessRequest input) {
        return applicationLookup.execute(input.applicationName())
                .map(application -> new AccessRequest(application.tenantId(), input.subject(), application.id(),
                        input.resourcePath(), input.action(), input.requestId(), input.correlationId(),
                        input.subjectUserId(), Set.of(), input.facts().timestamp(), input.facts().environment(),
                        input.facts().method(), input.facts().channel()))
                .flatMap(authorizeUseCase::execute)
                .onErrorResume(error -> error instanceof ApplicationNotFoundException
                                || error instanceof AmbiguousApplicationNameException,
                        error -> Mono.just(deny(input, ReasonCode.TENANT_MISMATCH)));
    }

    private AccessDecision deny(InternalAccessRequest input, ReasonCode reasonCode) {
        return new AccessDecision(identifiers.next(), DecisionState.DENY, reasonCode, List.of(),
                input.requestId(), input.correlationId(), time.now());
    }
}
