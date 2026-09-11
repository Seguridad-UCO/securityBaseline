package co.edu.uco.seguridad.pep.ingress.application.usecase;

import co.edu.uco.seguridad.pep.application.contract.ReactiveOperation;
import co.edu.uco.seguridad.pep.ingress.application.port.primary.dto.request.RegisterIntegrationRequest;
import co.edu.uco.seguridad.pep.ingress.application.port.primary.dto.response.RegisteredIntegrationResponse;

/** Puerto primario para registrar una integración sin acoplarse a HTTP o almacenamiento. */
public interface RegisterIntegrationUseCase
        extends ReactiveOperation<RegisterIntegrationRequest, RegisteredIntegrationResponse> {
}
