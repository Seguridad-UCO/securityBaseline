package co.edu.uco.seguridad.pep.normalization.application.rulesvalidator;

import co.edu.uco.seguridad.pep.application.contract.ReactiveOperationWithoutResult;
import co.edu.uco.seguridad.pep.normalization.application.port.primary.dto.request.NormalizeAccessRequest;

/**
 * Punto de entrada único para las reglas previas a la normalización.
 */
public interface NormalizeAccessRulesValidator extends ReactiveOperationWithoutResult<NormalizeAccessRequest> {
}
