package co.edu.uco.seguridad.pdp.roles.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleNamesLookupValidator;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class RoleNamesLookupValidatorImpl implements RoleNamesLookupValidator {

    private final RoleRepository repository;

    public RoleNamesLookupValidatorImpl(RoleRepository repository) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.ROLE_REPOSITORY);
    }

    @Override
    public Mono<Set<String>> execute(Set<RoleId> input) {
        return Flux.fromIterable(input)
                .flatMap(repository::findById)
                .map(role -> role.name().value())
                .collect(Collectors.toSet());
    }
}
