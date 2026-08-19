package co.edu.uco.seguridad.pdp.resources.application.rule.impl;

import co.edu.uco.seguridad.pdp.resources.application.exception.DuplicateProtectedResourceException;
import co.edu.uco.seguridad.pdp.resources.application.model.ProtectedResourceRegistration;
import co.edu.uco.seguridad.pdp.resources.application.port.primary.dto.request.RegisterProtectedApplicationRequest;
import co.edu.uco.seguridad.pdp.resources.application.port.secondary.repository.ProtectedResourceRepository;
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
    public Mono<Void> execute(ProtectedResourceRegistration registration) {
        RegisterProtectedApplicationRequest dto = registration.dto();
        return repository.existsGrant(dto.tenantId(), registration.application().id(), dto.resourceCode(), dto.action())
                .filter(Boolean::booleanValue)
                .flatMap(exists -> Mono.<Void>error(
                        new DuplicateProtectedResourceException(dto.resourceCode(), dto.action())));
    }
}
