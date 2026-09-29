package co.edu.uco.seguridad.pep.normalization.application.rule;

import co.edu.uco.seguridad.pep.application.contract.OperationWithoutResult;
import co.edu.uco.seguridad.pep.normalization.application.port.primary.dto.request.NormalizeAccessRequest;

/**
 * Regla que garantiza que el adaptador HTTP entregó todos los datos del contrato v1.
 */
public interface NormalizeAccessRequestMustBeCompleteRule extends OperationWithoutResult<NormalizeAccessRequest> {
}
