package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.port.ServiceIdentityRepository;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.request.CreateServiceTechnicalRoleRequest;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceTechnicalRoleDefinition;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Locale;

/**
 * Crea un catálogo técnico que sólo puede contener recursos de su aplicación propietaria.
 */
public final class CreateServiceTechnicalRoleUseCaseImpl implements CreateServiceTechnicalRoleUseCase {
    private final PrincipalMustBeApplicationAdministratorValidator administrators;
    private final ServiceIdentityRepository identities;
    private final ProtectedResourceRepository resources;
    private final IdentifierGenerator ids;
    private final TimeProvider time;

    public CreateServiceTechnicalRoleUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator administrators, ServiceIdentityRepository identities,
                                                 ProtectedResourceRepository resources, IdentifierGenerator ids, TimeProvider time) {
        this.administrators = administrators;
        this.identities = identities;
        this.resources = resources;
        this.ids = ids;
        this.time = time;
    }

    @Override
    public Mono<ServiceTechnicalRoleDefinition> execute(CreateServiceTechnicalRoleRequest input) {
        String code = input.code().trim().toUpperCase(Locale.ROOT);
        return administrators.execute(input.administration()).then(identities.findTechnicalRoles(input.administration().tenantId(), input.administration().applicationId())
                        .any(role -> role.code().equalsIgnoreCase(code))
                        .flatMap(duplicate -> duplicate ? Mono.error(new IllegalArgumentException("ya existe un rol técnico con ese código en la aplicación")) : Mono.empty()))
                .thenMany(Flux.fromIterable(input.resourceIds())
                        .flatMap(resourceId -> resources.findByIdForTenant(resourceId, input.administration().tenantId())
                                .filter(resource -> resource.applicationId().equals(input.administration().applicationId()))
                                .switchIfEmpty(Mono.error(new IllegalArgumentException("un rol técnico sólo puede agrupar recursos de su aplicación")))))
                .then(Mono.defer(() -> {
                    var role = new ServiceTechnicalRoleDefinition(ids.next(), input.administration().tenantId(), input.administration().applicationId(), code, input.name().trim(), time.now());
                    return identities.saveTechnicalRole(role).flatMap(saved -> identities.replaceTechnicalRoleResources(saved.tenantId(), saved.id(), input.resourceIds()).thenReturn(saved));
                }));
    }
}
