package co.edu.uco.seguridad.pdp.profiles.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ListProfilesRequest;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.ListProfilesUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class ListProfilesUseCaseImpl implements ListProfilesUseCase {

    private final ProfileRepository repository;

    public ListProfilesUseCaseImpl(ProfileRepository repository) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.PROFILE_REPOSITORY);
    }

    @Override
    public Mono<ResultPage<ProfileResponse>> execute(ListProfilesRequest input) {
        return repository.findBy(input.criteria(), input.window())
                .map(page -> page.map(profile -> new ProfileResponse(profile.id(), profile.name(), profile.scope(),
                        profile.roles(), profile.registeredAt())));
    }
}
