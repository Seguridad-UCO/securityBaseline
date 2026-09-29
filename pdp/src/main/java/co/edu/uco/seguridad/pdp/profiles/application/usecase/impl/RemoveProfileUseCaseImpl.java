package co.edu.uco.seguridad.pdp.profiles.application.usecase.impl;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.RemoveProfileUseCase;
import co.edu.uco.seguridad.pdp.profiles.domain.exception.ProfileNotFoundException;
import co.edu.uco.seguridad.pdp.commons.exception.CatalogItemInUseException;
import reactor.core.publisher.Mono;
public final class RemoveProfileUseCaseImpl implements RemoveProfileUseCase {
    private final ProfileRepository repository;
    public RemoveProfileUseCaseImpl(ProfileRepository repository) { this.repository = repository; }
    @Override public Mono<Void> execute(Request input) { return repository.findByIdForTenant(input.profileId(), input.tenantId())
            .switchIfEmpty(Mono.error(() -> new ProfileNotFoundException(input.profileId())))
            .flatMap(profile -> profile.roles().isEmpty()
                    ? repository.deleteById(profile.id())
                    : Mono.error(new CatalogItemInUseException("el perfil"))); }
}
