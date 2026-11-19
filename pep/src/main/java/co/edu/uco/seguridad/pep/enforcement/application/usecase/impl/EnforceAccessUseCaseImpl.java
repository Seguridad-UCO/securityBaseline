package co.edu.uco.seguridad.pep.enforcement.application.usecase.impl;

import co.edu.uco.seguridad.pep.commons.AccessDecision;
import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.enforcement.application.port.primary.dto.request.EnforceAccessRequest;
import co.edu.uco.seguridad.pep.enforcement.application.port.secondary.DecisionPort;
import co.edu.uco.seguridad.pep.enforcement.application.rulesvalidator.EnforceAccessRulesValidator;
import co.edu.uco.seguridad.pep.enforcement.application.usecase.EnforceAccessUseCase;
import co.edu.uco.seguridad.pep.enforcement.domain.DecisionApplicability;
import reactor.core.publisher.Mono;

public final class EnforceAccessUseCaseImpl implements EnforceAccessUseCase {

    private final DecisionPort decisions;
    private final EnforceAccessRulesValidator rules;

    public EnforceAccessUseCaseImpl(DecisionPort decisions, EnforceAccessRulesValidator rules) {
        this.decisions = decisions;
        this.rules = rules;
    }

    @Override
    public Mono<AccessDecision> execute(EnforceAccessRequest input) {
        return rules.execute(input).then(decisions.execute(input))
                .switchIfEmpty(Mono.error(new EnforcementFailure(
                        EnforcementFailure.Kind.UNAVAILABLE, "PDP_EMPTY_RESPONSE")))
                .doOnNext(decision -> DecisionApplicability.verify(input.accessRequest(), decision));
    }
}
