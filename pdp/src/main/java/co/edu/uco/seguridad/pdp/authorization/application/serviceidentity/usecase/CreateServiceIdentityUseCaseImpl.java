package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.port.ServiceClientProvisioner;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.port.ServiceIdentityRepository;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.request.CreateServiceIdentityRequest;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.response.ServiceIdentityResponse;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.service.ServiceIdentityAdministrationAuditor;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceIdentity;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceIdentityId;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceIdentityStatus;
import co.edu.uco.seguridad.shared.audit.AdministrationOperation;
import co.edu.uco.seguridad.shared.audit.AdministrationOutcome;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

import java.util.Locale;

/**
 * Crea el cliente en Keycloak antes de persistir la identidad: sin cliente no existe una identidad utilizable.
 */
public final class CreateServiceIdentityUseCaseImpl implements CreateServiceIdentityUseCase {
    private final PrincipalMustBeApplicationAdministratorValidator administrators;
    private final ServiceClientProvisioner clients;
    private final ServiceIdentityRepository identities;
    private final IdentifierGenerator ids;
    private final TimeProvider time;
    private final ServiceIdentityAdministrationAuditor auditor;

    public CreateServiceIdentityUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator administrators,
                                            ServiceClientProvisioner clients, ServiceIdentityRepository identities,
                                            IdentifierGenerator ids, TimeProvider time, ServiceIdentityAdministrationAuditor auditor) {
        this.administrators = administrators;
        this.clients = clients;
        this.identities = identities;
        this.ids = ids;
        this.time = time;
        this.auditor = auditor;
    }

    @Override
    public Mono<ServiceIdentityResponse> execute(CreateServiceIdentityRequest input) {
        return administrators.execute(input.administration()).then(Mono.defer(() -> {
            String suffix = input.name().trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]+", "-");
            if (suffix.isBlank()) return Mono.error(new IllegalArgumentException("nombre de identidad inválido"));
            String clientId = "svc-" + input.administration().applicationId().value() + "-" + suffix;
            return clients.provision(clientId).flatMap(client -> {
                ServiceIdentity identity = new ServiceIdentity(new ServiceIdentityId(ids.next()), input.administration().tenantId(),
                        input.administration().applicationId(), input.name(), client.clientId(), ServiceIdentityStatus.ACTIVE, time.now());
                return identities.saveIdentity(identity)
                        .map(saved -> new ServiceIdentityResponse(saved, client.secret()))
                        .onErrorResume(error -> clients.deprovision(client.clientId())
                                .onErrorResume(compensationError -> Mono.empty())
                                .then(Mono.error(error)));
            });
        })).flatMap(response -> auditor.record(input.administration(), AdministrationOperation.SERVICE_IDENTITY_CREATED,
                AdministrationOutcome.ALLOWED).thenReturn(response)).onErrorResume(error -> auditor.record(
                        input.administration(), AdministrationOperation.SERVICE_IDENTITY_CREATED, AdministrationOutcome.DENIED)
                .then(Mono.error(error)));
    }
}
