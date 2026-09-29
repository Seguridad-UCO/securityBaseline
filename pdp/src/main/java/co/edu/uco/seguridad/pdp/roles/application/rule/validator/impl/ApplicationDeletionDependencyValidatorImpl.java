package co.edu.uco.seguridad.pdp.roles.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.commons.exception.CatalogItemInUseException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.ApplicationDeletionDependencyValidator;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import reactor.core.publisher.Mono;

public final class ApplicationDeletionDependencyValidatorImpl implements ApplicationDeletionDependencyValidator {
    private final RoleRepository repository;
    public ApplicationDeletionDependencyValidatorImpl(RoleRepository repository) { this.repository = repository; }
    @Override public Mono<Void> execute(ApplicationId id) { return repository.existsByApplicationId(id)
            .flatMap(inUse -> inUse ? Mono.error(new CatalogItemInUseException("la aplicación")) : Mono.empty()); }
}
