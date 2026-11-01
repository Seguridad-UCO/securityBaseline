package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.port.ServiceIdentityRepository;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.request.ChangeServiceTechnicalRoleResourceRequest;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import reactor.core.publisher.Mono;

final class ChangeServiceTechnicalRoleResourceUseCaseSupport {
    private ChangeServiceTechnicalRoleResourceUseCaseSupport() { }
    static Mono<Void> validate(ChangeServiceTechnicalRoleResourceRequest input, PrincipalMustBeApplicationAdministratorValidator administrators,
                               ServiceIdentityRepository identities, ProtectedResourceRepository resources) {
        return administrators.execute(input.administration())
                .then(identities.findTechnicalRole(input.administration().tenantId(), input.technicalRoleId())
                        .filter(role -> role.applicationId().equals(input.administration().applicationId()))
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("el rol técnico no pertenece a la aplicación administrada"))))
                .then(resources.findByIdForTenant(input.resourceId(), input.administration().tenantId())
                        .filter(resource -> resource.applicationId().equals(input.administration().applicationId()))
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("el recurso no pertenece a la aplicación administrada"))))
                .then();
    }
}
