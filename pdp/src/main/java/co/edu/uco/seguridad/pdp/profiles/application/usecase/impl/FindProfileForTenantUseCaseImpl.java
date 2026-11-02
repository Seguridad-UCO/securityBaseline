package co.edu.uco.seguridad.pdp.profiles.application.usecase.impl;

import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.FindProfileForTenantRequest;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.FindProfileForTenantUseCase;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class FindProfileForTenantUseCaseImpl implements FindProfileForTenantUseCase {
    private final ProfileRepository profiles;

    public FindProfileForTenantUseCaseImpl(ProfileRepository profiles) {
        this.profiles = Objects.requireNonNull(profiles);
    }

    @Override
    public Mono<ProfileResponse> execute(FindProfileForTenantRequest input) {
        return profiles.findByIdForTenant(input.profileId(), input.tenantId()).map(profile -> new ProfileResponse(
                profile.id(), profile.name(), profile.scope(), profile.roles(), profile.registeredAt()));
    }
}
