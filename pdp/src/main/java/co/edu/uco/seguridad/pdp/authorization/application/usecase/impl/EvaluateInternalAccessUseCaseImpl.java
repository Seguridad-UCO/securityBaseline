package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.InternalAccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AccessDecision;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AuthorizeUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.EvaluateInternalAccessUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class EvaluateInternalAccessUseCaseImpl implements EvaluateInternalAccessUseCase {

    private final ApplicationOwnerLookupValidator ownerLookup;
    private final AuthorizeUseCase authorizeUseCase;

    public EvaluateInternalAccessUseCaseImpl(ApplicationOwnerLookupValidator ownerLookup,
            AuthorizeUseCase authorizeUseCase) {
        this.ownerLookup = Objects.requireNonNull(ownerLookup, RequiredArgumentMessages.APPLICATION_OWNER_LOOKUP_VALIDATOR);
        this.authorizeUseCase = Objects.requireNonNull(authorizeUseCase, RequiredArgumentMessages.AUTHORIZE_USE_CASE);
    }

    @Override
    public Mono<AccessDecision> execute(InternalAccessRequest input) {
        throw new UnsupportedOperationException("pendiente: HU-003");
    }
}
