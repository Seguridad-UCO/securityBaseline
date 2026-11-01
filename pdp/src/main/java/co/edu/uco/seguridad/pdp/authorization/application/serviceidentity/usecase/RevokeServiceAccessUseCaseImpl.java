package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.port.ServiceIdentityRepository;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.request.RevokeServiceAccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.service.ServiceIdentityAdministrationAuditor;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceAccessGrant;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceAccessStatus;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import co.edu.uco.seguridad.shared.audit.AdministrationOperation;
import co.edu.uco.seguridad.shared.audit.AdministrationOutcome;
import reactor.core.publisher.Mono;

/** El dueño del recurso destino retira un permiso vigente sin borrar su evidencia. */
public final class RevokeServiceAccessUseCaseImpl implements RevokeServiceAccessUseCase {
    private final PrincipalMustBeApplicationAdministratorValidator administrators;
    private final ServiceIdentityRepository identities;
    private final TimeProvider time;
    private final ServiceIdentityAdministrationAuditor auditor;

    public RevokeServiceAccessUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator administrators,
                                          ServiceIdentityRepository identities, TimeProvider time, ServiceIdentityAdministrationAuditor auditor) {
        this.administrators = administrators;
        this.identities = identities;
        this.time = time;
        this.auditor = auditor;
    }

    @Override public Mono<ServiceAccessGrant> execute(RevokeServiceAccessRequest input) {
        return administrators.execute(input.administration()).then(identities.findGrant(input.administration().tenantId(), input.grantId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("permiso inexistente")))
                .filter(grant -> grant.targetApplicationId().equals(input.administration().applicationId())
                        && grant.status() == ServiceAccessStatus.APPROVED)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("el permiso no está aprobado para esta aplicación")))
                .map(grant -> grant.revoke(time.now()))
                .flatMap(identities::saveGrant))
                .flatMap(grant -> auditor.record(input.administration(), AdministrationOperation.SERVICE_ACCESS_REVOKED,
                        AdministrationOutcome.ALLOWED).thenReturn(grant)).onErrorResume(error -> auditor.record(
                        input.administration(), AdministrationOperation.SERVICE_ACCESS_REVOKED, AdministrationOutcome.DENIED)
                        .then(Mono.error(error)));
    }
}
