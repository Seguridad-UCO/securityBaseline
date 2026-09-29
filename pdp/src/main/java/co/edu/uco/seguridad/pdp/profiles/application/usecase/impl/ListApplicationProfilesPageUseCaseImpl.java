package co.edu.uco.seguridad.pdp.profiles.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ListApplicationProfilesPageRequest;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.ListApplicationProfilesPageUseCase;
import co.edu.uco.seguridad.pdp.profiles.domain.ProfileCriteria;
import reactor.core.publisher.Mono;

public final class ListApplicationProfilesPageUseCaseImpl implements ListApplicationProfilesPageUseCase {
    private final ProfileRepository repository;
    public ListApplicationProfilesPageUseCaseImpl(ProfileRepository repository) { this.repository = repository; }
    @Override public Mono<ResultPage<ProfileResponse>> execute(ListApplicationProfilesPageRequest request) {
        return repository.findBy(ProfileCriteria.ofApplication(request.tenantId(), request.applicationId()), request.window())
                .map(page -> page.map(profile -> new ProfileResponse(profile.id(), profile.name(), profile.scope(), profile.roles(), profile.registeredAt())));
    }
}
