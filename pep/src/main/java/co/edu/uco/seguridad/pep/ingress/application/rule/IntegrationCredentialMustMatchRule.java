package co.edu.uco.seguridad.pep.ingress.application.rule;

import co.edu.uco.seguridad.pep.application.contract.ReactiveOperationWithoutResult;
import co.edu.uco.seguridad.pep.ingress.application.port.primary.dto.request.RegisterIntegrationRequest;

public interface IntegrationCredentialMustMatchRule extends ReactiveOperationWithoutResult<RegisterIntegrationRequest> {
}
