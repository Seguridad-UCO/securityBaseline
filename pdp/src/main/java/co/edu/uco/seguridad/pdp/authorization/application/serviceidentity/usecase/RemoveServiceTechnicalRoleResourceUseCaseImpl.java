package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.port.ServiceIdentityRepository;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.request.ChangeServiceTechnicalRoleResourceRequest;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import reactor.core.publisher.Mono;

public final class RemoveServiceTechnicalRoleResourceUseCaseImpl implements RemoveServiceTechnicalRoleResourceUseCase {
    private final PrincipalMustBeApplicationAdministratorValidator administrators;
    private final ServiceIdentityRepository identities;
    private final ProtectedResourceRepository resources;

    public RemoveServiceTechnicalRoleResourceUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator administrators, ServiceIdentityRepository identities, ProtectedResourceRepository resources) {
        this.administrators = administrators;
        this.identities = identities;
        this.resources = resources;
    }

    @Override
    public Mono<Void> execute(ChangeServiceTechnicalRoleResourceRequest input) {
        return ChangeServiceTechnicalRoleResourceUseCaseSupport.validate(input, administrators, identities, resources).then(identities.removeTechnicalRoleResource(input.administration().tenantId(), input.technicalRoleId(), input.resourceId()));
    }
}
