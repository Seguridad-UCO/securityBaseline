package co.edu.uco.seguridad.pdp.authorization.infrastructure.serviceidentity.web;

import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceIdentity;

import java.time.Instant;

record ServiceIdentityWebResponse(String id, String consumerApplicationId, String name, String clientId,
                                  String status, Instant registeredAt) {
    static ServiceIdentityWebResponse from(ServiceIdentity identity) {
        return new ServiceIdentityWebResponse(identity.id().value().toString(), identity.consumerApplicationId().value().toString(),
                identity.name(), identity.clientId(), identity.status().name(), identity.registeredAt());
    }
}
