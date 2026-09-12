package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.InternalAccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AccessDecision;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AuthorizeUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.EvaluateInternalAccessUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

import java.util.Objects;

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
        throw new UnsupportedOperationException("pendiente: HU-003");
    }
}
