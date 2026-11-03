package co.edu.uco.seguridad.pdp.roles.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.commons.exception.CatalogItemInUseException;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.ResourceDeletionDependencyValidator;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import reactor.core.publisher.Mono;

public final class ResourceDeletionDependencyValidatorImpl implements ResourceDeletionDependencyValidator {
    private final RoleRepository repository;

    public ResourceDeletionDependencyValidatorImpl(RoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Void> execute(ResourceId id) {
        return repository.existsByResourceId(id)
                .flatMap(inUse -> inUse ? Mono.error(new CatalogItemInUseException("el recurso")) : Mono.empty());
    }
}
