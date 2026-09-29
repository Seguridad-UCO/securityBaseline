package co.edu.uco.seguridad.pdp.profiles.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.commons.exception.CatalogItemInUseException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.ApplicationDeletionDependencyValidator;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import reactor.core.publisher.Mono;

public final class ApplicationDeletionDependencyValidatorImpl implements ApplicationDeletionDependencyValidator {
    private final ProfileRepository repository;
    public ApplicationDeletionDependencyValidatorImpl(ProfileRepository repository) { this.repository = repository; }
    @Override public Mono<Void> execute(ApplicationId id) { return repository.existsByApplicationId(id)
            .flatMap(inUse -> inUse ? Mono.error(new CatalogItemInUseException("la aplicación")) : Mono.empty()); }
}
