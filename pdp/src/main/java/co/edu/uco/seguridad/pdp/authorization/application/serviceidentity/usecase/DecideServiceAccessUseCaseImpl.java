package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.port.ServiceIdentityRepository;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.request.DecideServiceAccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.service.ServiceIdentityAdministrationAuditor;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceAccessGrant;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceAccessStatus;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import co.edu.uco.seguridad.shared.audit.AdministrationOperation;
import co.edu.uco.seguridad.shared.audit.AdministrationOutcome;
import reactor.core.publisher.Mono;

/** Solo el administrador del dueño del recurso destino aprueba o rechaza la solicitud. */
public final class DecideServiceAccessUseCaseImpl implements DecideServiceAccessUseCase {
    private final PrincipalMustBeApplicationAdministratorValidator administrators;
    private final ServiceIdentityRepository identities;
    private final TimeProvider time;
    private final ServiceIdentityAdministrationAuditor auditor;
    public DecideServiceAccessUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator administrators,
                                          ServiceIdentityRepository identities, TimeProvider time, ServiceIdentityAdministrationAuditor auditor) { this.administrators=administrators; this.identities=identities; this.time=time; this.auditor=auditor; }
    @Override public Mono<ServiceAccessGrant> execute(DecideServiceAccessRequest input) {
        return administrators.execute(input.administration()).then(identities.findGrant(input.administration().tenantId(), input.grantId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("solicitud inexistente")))
                .filter(grant -> grant.targetApplicationId().equals(input.administration().applicationId())
                        && grant.status() == ServiceAccessStatus.PENDING)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("la solicitud no está pendiente para esta aplicación")))
                .map(grant -> input.approve() ? grant.approve(time.now()) : grant.reject(time.now()))
                .flatMap(identities::saveGrant))
                .flatMap(grant -> auditor.record(input.administration(), input.approve()
                        ? AdministrationOperation.SERVICE_ACCESS_APPROVED : AdministrationOperation.SERVICE_ACCESS_REJECTED,
                        AdministrationOutcome.ALLOWED).thenReturn(grant)).onErrorResume(error -> auditor.record(
                        input.administration(), input.approve() ? AdministrationOperation.SERVICE_ACCESS_APPROVED
                                : AdministrationOperation.SERVICE_ACCESS_REJECTED, AdministrationOutcome.DENIED).then(Mono.error(error)));
    }
}
