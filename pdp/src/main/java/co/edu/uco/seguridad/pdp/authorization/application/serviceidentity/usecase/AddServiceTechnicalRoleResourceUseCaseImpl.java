package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.port.ServiceIdentityRepository;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.request.ChangeServiceTechnicalRoleResourceRequest;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import reactor.core.publisher.Mono;

public final class AddServiceTechnicalRoleResourceUseCaseImpl implements AddServiceTechnicalRoleResourceUseCase {
    private final PrincipalMustBeApplicationAdministratorValidator administrators;
    private final ServiceIdentityRepository identities;
    private final ProtectedResourceRepository resources;

    public AddServiceTechnicalRoleResourceUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator administrators, ServiceIdentityRepository identities, ProtectedResourceRepository resources) {
        this.administrators = administrators;
        this.identities = identities;
        this.resources = resources;
    }

    @Override
    public Mono<Void> execute(ChangeServiceTechnicalRoleResourceRequest input) {
        return ChangeServiceTechnicalRoleResourceUseCaseSupport.validate(input, administrators, identities, resources).then(identities.hasTechnicalRoleResource(input.administration().tenantId(), input.technicalRoleId(), input.resourceId()).flatMap(exists -> exists ? Mono.empty() : identities.addTechnicalRoleResource(input.administration().tenantId(), input.technicalRoleId(), input.resourceId())));
    }
}
