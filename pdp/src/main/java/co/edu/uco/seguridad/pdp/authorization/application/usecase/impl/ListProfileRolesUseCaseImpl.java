package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ListProfileRolesRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.ListProfileRolesUseCase;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.domain.exception.ProfileNotFoundException;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Locale;

public final class ListProfileRolesUseCaseImpl implements ListProfileRolesUseCase {
    private final PrincipalMustBeApplicationAdministratorValidator gate;
    private final ProfileRepository profiles;
    private final RoleRepository roles;

    public ListProfileRolesUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator gate, ProfileRepository profiles, RoleRepository roles) {
        this.gate = gate;
        this.profiles = profiles;
        this.roles = roles;
    }

    @Override
    public Mono<ResultPage<RoleResponse>> execute(ListProfileRolesRequest request) {
        return gate.execute(request.administration()).then(profiles.findByIdForTenant(request.profileId(), request.administration().tenantId())
                .switchIfEmpty(Mono.error(new ProfileNotFoundException(request.profileId())))
                .filter(profile -> profile.scope().applicationId().filter(request.administration().applicationId()::equals).isPresent())
                .switchIfEmpty(Mono.error(new ProfileNotFoundException(request.profileId())))
                .flatMap(profile -> {
                    String query = request.query().toLowerCase(Locale.ROOT);
                    return Flux.fromIterable(profile.roles().stream().sorted((a, b) -> a.value().toString().compareTo(b.value().toString())).toList())
                            .concatMap(id -> roles.findByIdForTenant(id, request.administration().tenantId()))
                            .filter(role -> query.isEmpty() || role.name().value().toLowerCase(Locale.ROOT).contains(query))
                            .map(role -> new RoleResponse(role.id(), role.name(), role.scope(), role.resources(), role.registeredAt()))
                            .collectList().map(filtered -> ResultPage.of(filtered.stream().skip(request.window().offset())
                                    .limit(request.window().limit()).toList(), filtered.size(), request.window()));
                }));
    }
}
