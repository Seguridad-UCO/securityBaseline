package co.edu.uco.seguridad.pdp.resources.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.ProtectedResourceLookup;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.ProtectedResourceMustExistValidator;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.domain.rule.ProtectedResourceMustExistRule;
import co.edu.uco.seguridad.pdp.resources.domain.rule.model.ProtectedResourceExistence;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class ProtectedResourceMustExistValidatorImpl implements ProtectedResourceMustExistValidator {

    private final ProtectedResourceRepository repository;
    private final ProtectedResourceMustExistRule mustExist;

    public ProtectedResourceMustExistValidatorImpl(ProtectedResourceRepository repository,
                                                   ProtectedResourceMustExistRule mustExist) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.PROTECTED_RESOURCE_REPOSITORY);
        this.mustExist = Objects.requireNonNull(mustExist, RequiredArgumentMessages.PROTECTED_RESOURCE_EXISTS_RULE);
    }

    @Override
    public Mono<Void> execute(ProtectedResourceLookup input) {
        return repository.existsByApplicationPathAndMethod(input.applicationId(), input.path(), input.method())
                .doOnNext(registered -> mustExist.execute(
                        new ProtectedResourceExistence(input.applicationId(), input.path(), input.method(), registered)))
                .then();
    }
}
