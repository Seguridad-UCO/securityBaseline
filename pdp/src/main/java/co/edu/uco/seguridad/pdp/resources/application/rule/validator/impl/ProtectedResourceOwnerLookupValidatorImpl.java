package co.edu.uco.seguridad.pdp.resources.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.ProtectedResourceOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.domain.exception.ProtectedResourceNotFoundException;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class ProtectedResourceOwnerLookupValidatorImpl implements ProtectedResourceOwnerLookupValidator {

    private final ProtectedResourceRepository repository;

    public ProtectedResourceOwnerLookupValidatorImpl(ProtectedResourceRepository repository) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.PROTECTED_RESOURCE_REPOSITORY);
    }

    @Override
    public Mono<ApplicationId> execute(ResourceId input) {
        return repository.findApplicationIdById(input)
                .switchIfEmpty(Mono.error(() -> new ProtectedResourceNotFoundException(input)));
    }
}
