package co.edu.uco.seguridad.pep.enforcement.application.port.secondary;

import co.edu.uco.seguridad.pep.application.contract.ReactiveOperation;
import co.edu.uco.seguridad.pep.commons.AccessDecision;
import co.edu.uco.seguridad.pep.enforcement.application.port.primary.dto.request.EnforceAccessRequest;

/**
 * Owned by the use case; infrastructure connects it to the decision-client module API.
 */
public interface DecisionPort extends ReactiveOperation<EnforceAccessRequest, AccessDecision> {
}
