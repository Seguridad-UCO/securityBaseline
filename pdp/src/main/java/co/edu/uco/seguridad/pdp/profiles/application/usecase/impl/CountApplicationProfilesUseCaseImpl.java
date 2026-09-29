package co.edu.uco.seguridad.pdp.profiles.application.usecase.impl;

import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ApplicationProfileCountRequest;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.CountApplicationProfilesUseCase;
import reactor.core.publisher.Mono;

public final class CountApplicationProfilesUseCaseImpl implements CountApplicationProfilesUseCase {
    private final ProfileRepository repository;

    public CountApplicationProfilesUseCaseImpl(ProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Long> execute(ApplicationProfileCountRequest request) {
        return repository.countByApplication(request.tenantId(), request.applicationId());
    }
}
