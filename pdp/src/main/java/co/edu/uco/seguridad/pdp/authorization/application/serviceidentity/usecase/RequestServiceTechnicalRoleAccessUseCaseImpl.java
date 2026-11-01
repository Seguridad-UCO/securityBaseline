package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.port.ServiceIdentityRepository;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.request.RequestServiceTechnicalRoleAccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.service.ServiceIdentityAdministrationAuditor;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.*;
import co.edu.uco.seguridad.shared.audit.AdministrationOperation;
import co.edu.uco.seguridad.shared.audit.AdministrationOutcome;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

/** Solicita un rol técnico sin convertirlo en un permiso compartido entre identidades. */
public final class RequestServiceTechnicalRoleAccessUseCaseImpl implements RequestServiceTechnicalRoleAccessUseCase {
    private final PrincipalMustBeApplicationAdministratorValidator administrators; private final ServiceIdentityRepository identities;
    private final IdentifierGenerator ids; private final TimeProvider time; private final ServiceIdentityAdministrationAuditor auditor;
    public RequestServiceTechnicalRoleAccessUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator administrators, ServiceIdentityRepository identities,
                                                         IdentifierGenerator ids, TimeProvider time, ServiceIdentityAdministrationAuditor auditor) {
        this.administrators=administrators; this.identities=identities; this.ids=ids; this.time=time; this.auditor=auditor;
    }
    @Override public Mono<ServiceAccessGrant> execute(RequestServiceTechnicalRoleAccessRequest input) {
        return administrators.execute(input.administration()).then(identities.findIdentity(input.administration().tenantId(), input.serviceIdentityId())
                .filter(identity -> identity.consumerApplicationId().equals(input.administration().applicationId()) && identity.status()==ServiceIdentityStatus.ACTIVE)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("la identidad no pertenece a la aplicación administrada")))
                .then(identities.findTechnicalRole(input.administration().tenantId(), input.technicalRoleId()))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("rol técnico inexistente")))
                .flatMap(role -> identities.hasPendingOrApprovedTechnicalRoleGrant(input.administration().tenantId(), input.serviceIdentityId(), role.id())
                        .flatMap(exists -> exists ? Mono.error(new IllegalArgumentException("ya existe una solicitud o permiso vigente para el rol técnico"))
                                : identities.saveGrant(new ServiceAccessGrant(ids.next(), input.administration().tenantId(), input.serviceIdentityId(), role.applicationId(),
                                        null, role.id(), role.code(), ServiceAccessStatus.PENDING, time.now(), null))))
                .flatMap(grant -> auditor.record(input.administration(), AdministrationOperation.SERVICE_ACCESS_REQUESTED, AdministrationOutcome.ALLOWED).thenReturn(grant))
                .onErrorResume(error -> auditor.record(input.administration(), AdministrationOperation.SERVICE_ACCESS_REQUESTED, AdministrationOutcome.DENIED).then(Mono.error(error))));
    }
}
