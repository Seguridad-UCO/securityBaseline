package co.edu.uco.seguridad.pep.ingress.application.rule;

import co.edu.uco.seguridad.pep.application.contract.OperationWithoutResult;
import co.edu.uco.seguridad.pep.ingress.application.port.primary.dto.request.RegisterIntegrationRequest;

public interface IntegrationRegistrationMustBeEnabledRule extends OperationWithoutResult<RegisterIntegrationRequest> {
}
