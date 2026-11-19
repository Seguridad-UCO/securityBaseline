package co.edu.uco.seguridad.pdp.profiles.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ProfileOwnershipQuery;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.ProfileRolesLookupValidator;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.domain.rule.ProfileMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.profiles.domain.rule.model.ProfileExistence;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Set;

public final class ProfileRolesLookupValidatorImpl implements ProfileRolesLookupValidator {

    private final ProfileRepository repository;
    private final ProfileMustExistForTenantRule mustExist;

    public ProfileRolesLookupValidatorImpl(ProfileRepository repository, ProfileMustExistForTenantRule mustExist) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.PROFILE_REPOSITORY);
        this.mustExist = Objects.requireNonNull(mustExist, RequiredArgumentMessages.PROFILE_EXISTS_RULE);
    }

    @Override
    public Mono<Set<RoleId>> execute(ProfileOwnershipQuery query) {
        return repository.findByIdForTenant(query.profileId(), query.tenantId())
                .doOnNext(profile -> mustExist.execute(new ProfileExistence(query.profileId(), query.tenantId(), true)))
                .switchIfEmpty(Mono.fromRunnable(() -> mustExist.execute(
                        new ProfileExistence(query.profileId(), query.tenantId(), false))))
                .map(profile -> profile.roles());
    }
}
