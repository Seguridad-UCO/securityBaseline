package co.edu.uco.seguridad.pdp.profiles.application.usecase.impl;

import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.AddRoleToProfileRequest;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.RemoveRoleFromProfileRequest;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.AddRoleToProfileRulesValidator;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.RemoveRoleFromProfileUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Reutiliza la comprobación de perfil, rol y compatibilidad de alcance de la asociación.
 */
public final class RemoveRoleFromProfileUseCaseImpl implements RemoveRoleFromProfileUseCase {
    private final AddRoleToProfileRulesValidator rules;
    private final ProfileRepository repository;

    public RemoveRoleFromProfileUseCaseImpl(AddRoleToProfileRulesValidator rules, ProfileRepository repository) {
        this.rules = Objects.requireNonNull(rules, RequiredArgumentMessages.ADD_ROLE_TO_PROFILE_RULES_VALIDATOR);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.PROFILE_REPOSITORY);
    }

    @Override
    public Mono<ProfileResponse> execute(RemoveRoleFromProfileRequest input) {
        return rules.execute(new AddRoleToProfileRequest(input.tenantId(), input.profileId(), input.roleId()))
                .map(profile -> profile.withoutRole(input.roleId())).flatMap(repository::save)
                .map(profile -> new ProfileResponse(profile.id(), profile.name(), profile.scope(), profile.roles(), profile.registeredAt()));
    }
}
