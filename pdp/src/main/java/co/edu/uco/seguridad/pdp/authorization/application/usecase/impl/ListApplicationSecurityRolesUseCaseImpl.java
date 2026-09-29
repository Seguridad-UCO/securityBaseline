package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ListApplicationSecurityRolesRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.ListApplicationSecurityRolesUseCase;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.ListApplicationRolesPageRequest;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.roles.application.usecase.ListApplicationRolesPageUseCase;
import reactor.core.publisher.Mono;

public final class ListApplicationSecurityRolesUseCaseImpl implements ListApplicationSecurityRolesUseCase {
    private final PrincipalMustBeApplicationAdministratorValidator gate;
    private final ListApplicationRolesPageUseCase roles;
    public ListApplicationSecurityRolesUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator gate,
                                                   ListApplicationRolesPageUseCase roles) { this.gate = gate; this.roles = roles; }
    @Override public Mono<ResultPage<RoleResponse>> execute(ListApplicationSecurityRolesRequest request) {
        var administration = request.administration();
        return gate.execute(administration).then(Mono.defer(() -> roles.execute(
                new ListApplicationRolesPageRequest(administration.tenantId(), administration.applicationId(), request.window()))));
    }
}
