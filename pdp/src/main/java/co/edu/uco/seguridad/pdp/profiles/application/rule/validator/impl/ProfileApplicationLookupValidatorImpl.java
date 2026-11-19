package co.edu.uco.seguridad.pdp.profiles.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ProfileOwnershipQuery;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.ProfileApplicationLookupValidator;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.domain.rule.ProfileMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.profiles.domain.rule.model.ProfileExistence;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;

/**
 * Implementación de {@link ProfileApplicationLookupValidator} (HU-019).
 */
public final class ProfileApplicationLookupValidatorImpl implements ProfileApplicationLookupValidator {

    private final ProfileRepository repository;
    private final ProfileMustExistForTenantRule mustExist;

    public ProfileApplicationLookupValidatorImpl(ProfileRepository repository, ProfileMustExistForTenantRule mustExist) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.PROFILE_REPOSITORY);
        this.mustExist = Objects.requireNonNull(mustExist, RequiredArgumentMessages.PROFILE_EXISTS_RULE);
    }

    @Override
    public Mono<Optional<ApplicationId>> execute(ProfileOwnershipQuery input) {
        return repository.findByIdForTenant(input.profileId(), input.tenantId())
                .doOnNext(profile -> mustExist.execute(new ProfileExistence(input.profileId(), input.tenantId(), true)))
                .switchIfEmpty(Mono.fromRunnable(
                        () -> mustExist.execute(new ProfileExistence(input.profileId(), input.tenantId(), false))))
                .map(profile -> profile.scope().applicationId());
    }
}
