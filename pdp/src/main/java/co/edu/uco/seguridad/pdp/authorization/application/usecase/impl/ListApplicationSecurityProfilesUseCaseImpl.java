package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ListApplicationSecurityProfilesRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.ListApplicationSecurityProfilesUseCase;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ListApplicationProfilesPageRequest;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.ListApplicationProfilesPageUseCase;
import reactor.core.publisher.Mono;

public final class ListApplicationSecurityProfilesUseCaseImpl implements ListApplicationSecurityProfilesUseCase {
    private final PrincipalMustBeApplicationAdministratorValidator gate;
    private final ListApplicationProfilesPageUseCase profiles;
    public ListApplicationSecurityProfilesUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator gate,
                                                      ListApplicationProfilesPageUseCase profiles) { this.gate = gate; this.profiles = profiles; }
    @Override public Mono<ResultPage<ProfileResponse>> execute(ListApplicationSecurityProfilesRequest request) {
        var administration = request.administration();
        return gate.execute(administration).then(Mono.defer(() -> profiles.execute(
                new ListApplicationProfilesPageRequest(administration.tenantId(), administration.applicationId(), request.window()))));
    }
}
