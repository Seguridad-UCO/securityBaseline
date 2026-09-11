package co.edu.uco.seguridad.pep.enforcement.application.rule;

import co.edu.uco.seguridad.pep.application.contract.OperationWithoutResult;
import co.edu.uco.seguridad.pep.enforcement.application.port.primary.dto.request.EnforceAccessRequest;

/** Regla que evita consultar al PDP con evidencia o metadatos incompletos. */
public interface EnforceAccessRequestMustBeCompleteRule extends OperationWithoutResult<EnforceAccessRequest> {
}
