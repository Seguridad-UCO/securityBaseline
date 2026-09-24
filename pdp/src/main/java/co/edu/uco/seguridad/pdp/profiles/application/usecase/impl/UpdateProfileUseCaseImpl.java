package co.edu.uco.seguridad.pdp.profiles.application.usecase.impl;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.UpdateProfileRequest;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.UpdateProfileUseCase;
import co.edu.uco.seguridad.pdp.profiles.domain.exception.DuplicateProfileNameException;
import co.edu.uco.seguridad.pdp.profiles.domain.exception.ProfileNotFoundException;
import reactor.core.publisher.Mono;
public final class UpdateProfileUseCaseImpl implements UpdateProfileUseCase {
    private final ProfileRepository repository;
    public UpdateProfileUseCaseImpl(ProfileRepository repository) { this.repository = repository; }
    @Override public Mono<ProfileResponse> execute(UpdateProfileRequest input) { return repository.findByIdForTenant(input.profileId(), input.tenantId())
            .switchIfEmpty(Mono.error(() -> new ProfileNotFoundException(input.profileId())))
            .flatMap(current -> repository.existsByNameInScope(input.name(), current.scope())
                    .filter(taken -> taken && !current.name().equals(input.name()))
                    .flatMap(taken -> Mono.<co.edu.uco.seguridad.pdp.profiles.domain.Profile>error(new DuplicateProfileNameException(input.name(), current.scope())))
                    .switchIfEmpty(repository.save(current.withName(input.name()))))
            .map(profile -> new ProfileResponse(profile.id(), profile.name(), profile.scope(), profile.roles(), profile.registeredAt())); }
}
