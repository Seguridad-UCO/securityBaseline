package co.edu.uco.seguridad.pdp.roles.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleResourcesLookupValidator;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Set;
import java.util.stream.Collectors;

public final class RoleResourcesLookupValidatorImpl implements RoleResourcesLookupValidator {
    private final RoleRepository repository;
    public RoleResourcesLookupValidatorImpl(RoleRepository repository) { this.repository = repository; }
    @Override public Mono<Set<ResourceId>> execute(Set<RoleId> ids) {
        return Flux.fromIterable(ids).flatMap(repository::findById).flatMap(role -> Flux.fromIterable(role.resources()))
                .collect(Collectors.toSet());
    }
}
