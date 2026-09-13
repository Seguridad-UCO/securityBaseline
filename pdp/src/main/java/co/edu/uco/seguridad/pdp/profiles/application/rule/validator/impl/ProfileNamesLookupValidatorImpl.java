package co.edu.uco.seguridad.pdp.profiles.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.ProfileNamesLookupValidator;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Set;
import java.util.stream.Collectors;

public final class ProfileNamesLookupValidatorImpl implements ProfileNamesLookupValidator {
    private final ProfileRepository repository;
    public ProfileNamesLookupValidatorImpl(ProfileRepository repository) { this.repository = repository; }
    @Override public Mono<Set<String>> execute(Set<ProfileId> ids) {
        return Flux.fromIterable(ids).flatMap(repository::findById).map(profile -> profile.name().value())
                .collect(Collectors.toSet());
    }
}
