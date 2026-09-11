package co.edu.uco.seguridad.pep.enforcement.application.usecase;

import co.edu.uco.seguridad.pep.commons.AccessDecision;
import co.edu.uco.seguridad.pep.application.contract.ReactiveOperation;
import co.edu.uco.seguridad.pep.enforcement.application.port.primary.dto.request.EnforceAccessRequest;

public interface EnforceAccessUseCase extends ReactiveOperation<EnforceAccessRequest, AccessDecision> {
}
