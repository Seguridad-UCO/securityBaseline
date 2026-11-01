package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.port;

import reactor.core.publisher.Mono;

/** Keycloak conserva el secreto; el PDP solo lo entrega una vez durante el alta o rotación. */
public interface ServiceClientProvisioner {
    Mono<ProvisionedServiceClient> provision(String clientId);
    Mono<Void> deprovision(String clientId);
    record ProvisionedServiceClient(String clientId, String secret) { }
}
