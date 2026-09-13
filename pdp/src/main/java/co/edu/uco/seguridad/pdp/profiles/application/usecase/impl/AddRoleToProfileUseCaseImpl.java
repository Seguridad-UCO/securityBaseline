package co.edu.uco.seguridad.pdp.profiles.application.usecase.impl;

import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.AddRoleToProfileRequest;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.AddRoleToProfileRulesValidator;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.AddRoleToProfileUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Transforma lo que el validador ya encontró (Profile.withRole) y lo guarda. No decide nada. */
public final class AddRoleToProfileUseCaseImpl implements AddRoleToProfileUseCase {

    private final AddRoleToProfileRulesValidator rules;
    private final ProfileRepository repository;

    public AddRoleToProfileUseCaseImpl(AddRoleToProfileRulesValidator rules, ProfileRepository repository) {
        this.rules = Objects.requireNonNull(rules, RequiredArgumentMessages.ADD_ROLE_TO_PROFILE_RULES_VALIDATOR);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.PROFILE_REPOSITORY);
    }

    @Override
    public Mono<ProfileResponse> execute(AddRoleToProfileRequest input) {
        return rules.execute(input)
                .map(profile -> profile.withRole(input.roleId()))
                .flatMap(repository::save)
                .map(profile -> new ProfileResponse(profile.id(), profile.name(), profile.scope(), profile.roles(),
                        profile.registeredAt()));
    }
}
