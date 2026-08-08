package co.edu.uco.seguridad.pdp.recursos.application.rule.impl;

import co.edu.uco.seguridad.pdp.recursos.application.exception.DuplicateProtectedResourceException;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.RegisterProtectedApplicationRequest;
import co.edu.uco.seguridad.pdp.recursos.application.port.secondary.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.recursos.application.rule.ProtectedResourceMustBeUniqueRule;
import co.edu.uco.seguridad.pdp.recursos.application.rule.ProtectedResourceRegistration;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class ProtectedResourceMustBeUniqueRuleImpl implements ProtectedResourceMustBeUniqueRule {

    private final ProtectedResourceRepository repository;

    public ProtectedResourceMustBeUniqueRuleImpl(ProtectedResourceRepository repository) {
        this.repository = Objects.requireNonNull(repository, "se requiere repositorio de recurso protegido");
    }

    @Override
    public Mono<Void> execute(ProtectedResourceRegistration registration) {
        RegisterProtectedApplicationRequest dto = registration.dto();
        return repository.existsGrant(registration.application().id(), dto.resourceCode(), dto.action())
                .filter(Boolean::booleanValue)
                .flatMap(exists -> Mono.<Void>error(
                        new DuplicateProtectedResourceException(dto.resourceCode(), dto.action())));
    }
}
