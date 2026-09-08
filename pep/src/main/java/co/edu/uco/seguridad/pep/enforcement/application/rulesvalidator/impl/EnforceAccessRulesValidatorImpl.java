package co.edu.uco.seguridad.pep.enforcement.application.rulesvalidator.impl;

import co.edu.uco.seguridad.pep.enforcement.application.port.primary.dto.request.EnforceAccessRequest;
import co.edu.uco.seguridad.pep.enforcement.application.rule.EnforceAccessRequestMustBeCompleteRule;
import co.edu.uco.seguridad.pep.enforcement.application.rulesvalidator.EnforceAccessRulesValidator;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class EnforceAccessRulesValidatorImpl implements EnforceAccessRulesValidator {
    private final EnforceAccessRequestMustBeCompleteRule requestMustBeComplete;

    public EnforceAccessRulesValidatorImpl(EnforceAccessRequestMustBeCompleteRule requestMustBeComplete) {
        this.requestMustBeComplete = Objects.requireNonNull(requestMustBeComplete);
    }

    @Override
    public Mono<Void> execute(EnforceAccessRequest input) {
        return Mono.fromRunnable(() -> requestMustBeComplete.execute(input));
    }
}
