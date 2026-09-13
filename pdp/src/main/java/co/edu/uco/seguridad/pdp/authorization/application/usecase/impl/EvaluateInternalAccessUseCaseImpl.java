package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
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

    private final ApplicationOwnerLookupValidator ownerLookup;
    private final AuthorizeUseCase authorizeUseCase;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public EvaluateInternalAccessUseCaseImpl(ApplicationOwnerLookupValidator ownerLookup,
            AuthorizeUseCase authorizeUseCase, IdentifierGenerator identifiers, TimeProvider time) {
        this.ownerLookup = Objects.requireNonNull(ownerLookup, RequiredArgumentMessages.APPLICATION_OWNER_LOOKUP_VALIDATOR);
        this.authorizeUseCase = Objects.requireNonNull(authorizeUseCase, RequiredArgumentMessages.AUTHORIZE_USE_CASE);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<AccessDecision> execute(InternalAccessRequest input) {
        // subjectUserId vacio a proposito (HU-008, fuera de alcance): el canal interno no tiene
        // identidad de usuario final que resolver todavia -- input.subject() es el JWT de evidencia
        // que autentica al PEP, no al usuario. Resolverlo exige cambiar contracts/pep-pdp/v1/.
        return ownerLookup.execute(input.applicationId())
                .map(tenantId -> new AccessRequest(tenantId, input.subject(), input.applicationId(),
                        input.resourcePath(), input.action(), input.requestId(), input.correlationId(),
                        Optional.empty(), Set.of(), input.facts().timestamp(), input.facts().environment(),
                        input.facts().method(), input.facts().channel()))
                .flatMap(authorizeUseCase::execute)
                .onErrorResume(ApplicationNotFoundException.class,
                        error -> Mono.just(deny(input, ReasonCode.TENANT_MISMATCH)));
    }

    private AccessDecision deny(InternalAccessRequest input, ReasonCode reasonCode) {
        return new AccessDecision(identifiers.next(), DecisionState.DENY, reasonCode, List.of(),
                input.requestId(), input.correlationId(), time.now());
    }
}
