package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ListApplicationAdministratorsRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationAdministratorWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

import java.util.List;

public interface AdministerApplicationAdministratorListInteractor
        extends ReactiveOperation<ListApplicationAdministratorsRawRequest, List<ApplicationAdministratorWebResponse>> {
}
