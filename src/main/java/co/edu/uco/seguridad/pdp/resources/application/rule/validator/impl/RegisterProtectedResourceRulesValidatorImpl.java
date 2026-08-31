package co.edu.uco.seguridad.pdp.resources.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.RegisterProtectedResourceRequest;
import co.edu.uco.seguridad.pdp.resources.application.rule.ProtectedResourceMustBeUniqueRule;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.RegisterProtectedResourceRulesValidator;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class RegisterProtectedResourceRulesValidatorImpl implements RegisterProtectedResourceRulesValidator {

    private final ProtectedResourceMustBeUniqueRule mustBeUnique;

    public RegisterProtectedResourceRulesValidatorImpl(ProtectedResourceMustBeUniqueRule mustBeUnique) {
        this.mustBeUnique = Objects.requireNonNull(mustBeUnique);
    }

    @Override
    public Mono<Void> execute(RegisterProtectedResourceRequest dto) {
        return mustBeUnique.execute(dto);
    }
}
