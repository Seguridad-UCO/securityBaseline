package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.RemoveApplicationAdministratorRawRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface AdministerApplicationAdministratorRemovalInteractor
        extends ReactiveOperation<RemoveApplicationAdministratorRawRequest, Void> {
}
