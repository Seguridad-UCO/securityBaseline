package co.edu.uco.seguridad.pdp.resources.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.RegisterProtectedResourceRequest;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.RegisterProtectedResourceRulesValidator;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.domain.rule.ProtectedResourceMustBeUniqueRule;
import co.edu.uco.seguridad.pdp.resources.domain.rule.model.ProtectedResourceAvailability;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * El validador resuelve contra el puerto lo que la regla necesita saber; la regla solo decide.
 */
public final class RegisterProtectedResourceRulesValidatorImpl implements RegisterProtectedResourceRulesValidator {

    private final ProtectedResourceMustBeUniqueRule mustBeUnique;
    private final ProtectedResourceRepository repository;

    public RegisterProtectedResourceRulesValidatorImpl(ProtectedResourceMustBeUniqueRule mustBeUnique,
                                                       ProtectedResourceRepository repository) {
        this.mustBeUnique = Objects.requireNonNull(mustBeUnique, RequiredArgumentMessages.UNIQUE_RULE);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.PROTECTED_RESOURCE_REPOSITORY);
    }

    @Override
    public Mono<Void> execute(RegisterProtectedResourceRequest dto) {
        return repository.existsByApplicationPathAndMethod(dto.applicationId(), dto.path(), dto.method())
                .doOnNext(registered -> mustBeUnique.execute(new ProtectedResourceAvailability(
                        dto.applicationId(), dto.path(), dto.method(), registered)))
                .then();
    }
}
