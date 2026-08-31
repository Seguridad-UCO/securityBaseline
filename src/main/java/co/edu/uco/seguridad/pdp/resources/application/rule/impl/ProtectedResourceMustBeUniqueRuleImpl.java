package co.edu.uco.seguridad.pdp.resources.application.rule.impl;

import co.edu.uco.seguridad.pdp.resources.application.exception.DuplicateProtectedResourceException;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.RegisterProtectedResourceRequest;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.application.rule.ProtectedResourceMustBeUniqueRule;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class ProtectedResourceMustBeUniqueRuleImpl implements ProtectedResourceMustBeUniqueRule {

    private final ProtectedResourceRepository repository;

    public ProtectedResourceMustBeUniqueRuleImpl(ProtectedResourceRepository repository) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.PROTECTED_RESOURCE_REPOSITORY);
    }

    @Override
    public Mono<Void> execute(RegisterProtectedResourceRequest dto) {
        return repository.existsByApplicationPathAndMethod(dto.applicationId(), dto.path(), dto.method())
                .filter(Boolean::booleanValue)
                .flatMap(exists -> Mono.<Void>error(
                        new DuplicateProtectedResourceException(dto.path(), dto.method())));
    }
}
