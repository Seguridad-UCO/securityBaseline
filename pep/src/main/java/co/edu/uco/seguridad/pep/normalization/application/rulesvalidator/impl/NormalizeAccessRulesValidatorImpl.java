package co.edu.uco.seguridad.pep.normalization.application.rulesvalidator.impl;

import co.edu.uco.seguridad.pep.normalization.application.port.primary.dto.request.NormalizeAccessRequest;
import co.edu.uco.seguridad.pep.normalization.application.rule.NormalizeAccessRequestMustBeCompleteRule;
import co.edu.uco.seguridad.pep.normalization.application.rulesvalidator.NormalizeAccessRulesValidator;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class NormalizeAccessRulesValidatorImpl implements NormalizeAccessRulesValidator {
    private final NormalizeAccessRequestMustBeCompleteRule requestMustBeComplete;

    public NormalizeAccessRulesValidatorImpl(NormalizeAccessRequestMustBeCompleteRule requestMustBeComplete) {
        this.requestMustBeComplete = Objects.requireNonNull(requestMustBeComplete);
    }

    @Override
    public Mono<Void> execute(NormalizeAccessRequest input) {
        return Mono.fromRunnable(() -> requestMustBeComplete.execute(input));
    }
}
