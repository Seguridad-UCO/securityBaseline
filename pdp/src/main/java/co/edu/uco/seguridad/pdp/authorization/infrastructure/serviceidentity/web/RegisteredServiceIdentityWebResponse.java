package co.edu.uco.seguridad.pdp.authorization.infrastructure.serviceidentity.web;

import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.response.ServiceIdentityResponse;

record RegisteredServiceIdentityWebResponse(ServiceIdentityWebResponse identity, String clientSecret) {
    static RegisteredServiceIdentityWebResponse from(ServiceIdentityResponse response) {
        return new RegisteredServiceIdentityWebResponse(ServiceIdentityWebResponse.from(response.identity()), response.clientSecret());
    }
}
