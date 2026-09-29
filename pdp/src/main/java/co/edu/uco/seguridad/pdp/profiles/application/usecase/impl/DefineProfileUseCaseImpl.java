package co.edu.uco.seguridad.pdp.profiles.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.DefineProfileRequest;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.DefineProfileRulesValidator;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.DefineProfileUseCase;
import co.edu.uco.seguridad.pdp.profiles.domain.Profile;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Construye un Profile desde cero: por eso IdentifierGenerator y TimeProvider van en la firma desde el primer día.
 */
public final class DefineProfileUseCaseImpl implements DefineProfileUseCase {

    private final DefineProfileRulesValidator rules;
    private final ProfileRepository repository;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public DefineProfileUseCaseImpl(DefineProfileRulesValidator rules, ProfileRepository repository,
                                    IdentifierGenerator identifiers, TimeProvider time) {
        this.rules = Objects.requireNonNull(rules, RequiredArgumentMessages.DEFINE_PROFILE_RULES_VALIDATOR);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.PROFILE_REPOSITORY);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<ProfileResponse> execute(DefineProfileRequest input) {
        return rules.execute(input)
                .then(Mono.defer(() -> repository.save(Profile.define(
                        new ProfileId(identifiers.next()), input.name(), input.scope(), time.now()))))
                .map(profile -> new ProfileResponse(profile.id(), profile.name(), profile.scope(), profile.roles(),
                        profile.registeredAt()));
    }
}
