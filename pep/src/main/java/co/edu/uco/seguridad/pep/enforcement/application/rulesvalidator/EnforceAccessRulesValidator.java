package co.edu.uco.seguridad.pep.enforcement.application.rulesvalidator;

import co.edu.uco.seguridad.pep.application.contract.ReactiveOperationWithoutResult;
import co.edu.uco.seguridad.pep.enforcement.application.port.primary.dto.request.EnforceAccessRequest;

/** Punto de entrada único para las reglas previas a solicitar una decisión. */
public interface EnforceAccessRulesValidator extends ReactiveOperationWithoutResult<EnforceAccessRequest> {
}
