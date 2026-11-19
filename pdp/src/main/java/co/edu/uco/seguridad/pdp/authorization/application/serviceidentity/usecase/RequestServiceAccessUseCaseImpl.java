package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.port.ServiceIdentityRepository;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.request.RequestServiceAccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.service.ServiceIdentityAdministrationAuditor;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.DuplicateServiceAccessGrantException;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceAccessGrant;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceAccessStatus;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceIdentityStatus;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceTechnicalRole;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.shared.audit.AdministrationOperation;
import co.edu.uco.seguridad.shared.audit.AdministrationOutcome;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

/**
 * El administrador del consumidor puede solicitar; no puede aprobar recursos que no son suyos.
 */
public final class RequestServiceAccessUseCaseImpl implements RequestServiceAccessUseCase {
    private final PrincipalMustBeApplicationAdministratorValidator administrators;
    private final ServiceIdentityRepository identities;
    private final ProtectedResourceRepository resources;
    private final IdentifierGenerator ids;
    private final TimeProvider time;
    private final ServiceIdentityAdministrationAuditor auditor;

    public RequestServiceAccessUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator administrators, ServiceIdentityRepository identities,
                                           ProtectedResourceRepository resources, IdentifierGenerator ids, TimeProvider time,
                                           ServiceIdentityAdministrationAuditor auditor) {
        this.administrators = administrators;
        this.identities = identities;
        this.resources = resources;
        this.ids = ids;
        this.time = time;
        this.auditor = auditor;
    }

    @Override
    public Mono<ServiceAccessGrant> execute(RequestServiceAccessRequest input) {
        return administrators.execute(input.administration()).then(identities.findIdentity(input.administration().tenantId(), input.serviceIdentityId())
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("identidad de servicio inexistente")))
                        .filter(identity -> identity.tenantId().equals(input.administration().tenantId())
                                && identity.consumerApplicationId().equals(input.administration().applicationId())
                                && identity.status() == ServiceIdentityStatus.ACTIVE)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("la identidad no pertenece a la aplicación administrada")))
                        .then(resources.findByIdForTenant(input.resourceId(), input.administration().tenantId()))
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("recurso destino inexistente")))
                        .flatMap(resource -> identities.hasPendingOrApprovedGrant(input.administration().tenantId(), input.serviceIdentityId(), resource.id())
                                .flatMap(exists -> exists ? Mono.error(new DuplicateServiceAccessGrantException("el recurso solicitado"))
                                        : identities.saveGrant(new ServiceAccessGrant(ids.next(), input.administration().tenantId(), input.serviceIdentityId(), resource.applicationId(),
                                        resource.id(), null, ServiceTechnicalRole.SERVICE_CONSUMER.name(), ServiceAccessStatus.PENDING, time.now(), null)))))
                .flatMap(grant -> auditor.record(input.administration(), AdministrationOperation.SERVICE_ACCESS_REQUESTED,
                        AdministrationOutcome.ALLOWED).thenReturn(grant)).onErrorResume(error -> auditor.record(
                                input.administration(), AdministrationOperation.SERVICE_ACCESS_REQUESTED, AdministrationOutcome.DENIED)
                        .then(Mono.error(error)));
    }
}
