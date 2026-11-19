package co.edu.uco.seguridad.pdp.profiles.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.commons.exception.CatalogItemInUseException;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.RoleDeletionDependencyValidator;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import reactor.core.publisher.Mono;

public final class RoleDeletionDependencyValidatorImpl implements RoleDeletionDependencyValidator {
    private final ProfileRepository repository;

    public RoleDeletionDependencyValidatorImpl(ProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Void> execute(RoleId id) {
        return repository.existsByRoleId(id)
                .flatMap(inUse -> inUse ? Mono.error(new CatalogItemInUseException("el rol")) : Mono.empty());
    }
}
