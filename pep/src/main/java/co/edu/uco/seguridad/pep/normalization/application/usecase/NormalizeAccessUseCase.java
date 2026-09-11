package co.edu.uco.seguridad.pep.normalization.application.usecase;

import co.edu.uco.seguridad.pep.commons.AccessRequest;
import co.edu.uco.seguridad.pep.application.contract.ReactiveOperation;
import co.edu.uco.seguridad.pep.normalization.application.port.primary.dto.request.NormalizeAccessRequest;

public interface NormalizeAccessUseCase extends ReactiveOperation<NormalizeAccessRequest, AccessRequest> {
}
