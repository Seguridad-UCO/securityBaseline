package co.edu.uco.seguridad.pdp.applications.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationNameLookupValidator;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.exception.ApplicationNotFoundException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class ApplicationNameLookupValidatorImpl implements ApplicationNameLookupValidator {

    private final ApplicationRepository repository;

    public ApplicationNameLookupValidatorImpl(ApplicationRepository repository) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.APPLICATION_REPOSITORY);
    }

    @Override
    public Mono<Application> execute(ApplicationName input) {
        return repository.findUniqueByName(input)
                .switchIfEmpty(Mono.error(() -> new ApplicationNotFoundException(input)));
    }
}
