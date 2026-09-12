package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ApplicationOwnershipQuery;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationMustExistForTenantValidator;
import co.edu.uco.seguridad.pdp.applications.domain.exception.ApplicationNotFoundException;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AccessDecision;
import co.edu.uco.seguridad.pdp.authorization.application.secondaryport.PolicyDecisionPort;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AuthorizeUseCase;
import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.ProtectedResourceLookup;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.ProtectedResourceMustExistValidator;
import co.edu.uco.seguridad.pdp.resources.domain.exception.ProtectedResourceNotFoundException;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * Orquesta: comprueba aplicacion (regla 1) y recurso (regla 2, corta si la 1 ya fallo), y delega
 * en {@link PolicyDecisionPort}. No decide nada: traduce el rechazo ya decidido por el validador
 * ajeno a un {@link ReasonCode}. Fail-closed: cualquier fallo tecnico (no de negocio) en la
 * resolucion del contexto se traduce a INDETERMINATE, nunca a un error que llegue al cliente.
 */
public final class AuthorizeUseCaseImpl implements AuthorizeUseCase {

    private final ApplicationMustExistForTenantValidator applicationMustExist;
    private final ProtectedResourceMustExistValidator resourceMustExist;
    private final PolicyDecisionPort policyDecisionPort;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public AuthorizeUseCaseImpl(ApplicationMustExistForTenantValidator applicationMustExist,
            ProtectedResourceMustExistValidator resourceMustExist, PolicyDecisionPort policyDecisionPort,
            IdentifierGenerator identifiers, TimeProvider time) {
        this.applicationMustExist = Objects.requireNonNull(applicationMustExist,
                RequiredArgumentMessages.APPLICATION_EXISTS_VALIDATOR);
        this.resourceMustExist = Objects.requireNonNull(resourceMustExist,
                RequiredArgumentMessages.PROTECTED_RESOURCE_EXISTS_VALIDATOR);
        this.policyDecisionPort = Objects.requireNonNull(policyDecisionPort,
                RequiredArgumentMessages.POLICY_DECISION_PORT);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<AccessDecision> execute(AccessRequest input) {
        // Cada paso posterior al primero va en Mono.defer: sin el, la llamada a .execute(...) se
        // evalua al construir la cadena (Java evalua argumentos antes de invocar .then), no cuando
        // Reactor suscribe — el mismo defecto que switchIfEmpty sin defer, aqui aplicado a .then.
        return applicationMustExist.execute(new ApplicationOwnershipQuery(input.tenantId(), input.applicationId()))
                .then(Mono.defer(() -> resourceMustExist.execute(
                        new ProtectedResourceLookup(input.applicationId(), input.resourcePath(), input.action()))))
                .then(Mono.defer(() -> policyDecisionPort.execute(input)))
                .onErrorResume(ApplicationNotFoundException.class,
                        error -> Mono.just(deny(input, ReasonCode.TENANT_MISMATCH)))
                .onErrorResume(ProtectedResourceNotFoundException.class,
                        error -> Mono.just(deny(input, ReasonCode.NO_APPLICABLE_POLICY)))
                .onErrorResume(error -> Mono.just(indeterminate(input)));
    }

    private AccessDecision deny(AccessRequest input, ReasonCode reasonCode) {
        return new AccessDecision(identifiers.next(), DecisionState.DENY, reasonCode, List.of(),
                input.requestId(), input.correlationId(), time.now());
    }

    private AccessDecision indeterminate(AccessRequest input) {
        return new AccessDecision(identifiers.next(), DecisionState.INDETERMINATE, ReasonCode.CONTEXT_UNAVAILABLE,
                List.of(), input.requestId(), input.correlationId(), time.now());
    }
}
