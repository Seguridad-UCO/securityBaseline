package co.edu.uco.seguridad.pep.ingress.application.rulesvalidator;

import co.edu.uco.seguridad.pep.application.contract.ReactiveOperationWithoutResult;
import co.edu.uco.seguridad.pep.ingress.application.port.primary.dto.request.RegisterIntegrationRequest;

/**
 * Punto de entrada único de las reglas que protegen el registro de integraciones.
 */
public interface RegisterIntegrationRulesValidator extends ReactiveOperationWithoutResult<RegisterIntegrationRequest> {
}
