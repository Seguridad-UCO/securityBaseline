package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveActiveRolesRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AdministrationDecision;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.ActiveRoleNamesLookupValidator;
import co.edu.uco.seguridad.pdp.authorization.application.secondaryport.AdministrationDecisionPort;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AuthorizeAdministrationUseCase;
import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementación de {@link AuthorizeAdministrationUseCase} (HU-009). Pendiente: resolver
 * {@code subjectRoles} vía {@link ActiveRoleNamesLookupValidator} (mismo puente que
 * {@code AuthorizeUseCaseImpl.resolveRoles}, con {@code applicationId} en vez de opcional), delegar
 * en {@link AdministrationDecisionPort}, y traducir cualquier error a
 * {@code AdministrationDecision(INDETERMINATE, CONTEXT_UNAVAILABLE)} — fail-closed, nunca propagar.
 */
public final class AuthorizeAdministrationUseCaseImpl implements AuthorizeAdministrationUseCase {

    private final ActiveRoleNamesLookupValidator rolesLookup;
    private final AdministrationDecisionPort policyDecisionPort;

    public AuthorizeAdministrationUseCaseImpl(ActiveRoleNamesLookupValidator rolesLookup,
            AdministrationDecisionPort policyDecisionPort) {
        this.rolesLookup = Objects.requireNonNull(rolesLookup, RequiredArgumentMessages.ACTIVE_ROLE_NAMES_LOOKUP_VALIDATOR);
        this.policyDecisionPort = Objects.requireNonNull(policyDecisionPort,
                RequiredArgumentMessages.ADMINISTRATION_DECISION_PORT);
    }

    @Override
    public Mono<AdministrationDecision> execute(AdministrationRequest input) {
        return rolesLookup.execute(new ResolveActiveRolesRequest(input.subjectUserId(), input.applicationId()))
                .map(input::withSubjectRoles)
                .flatMap(policyDecisionPort::execute)
                .onErrorResume(error -> Mono.just(new AdministrationDecision(DecisionState.INDETERMINATE,
                        ReasonCode.CONTEXT_UNAVAILABLE)));
    }
}
