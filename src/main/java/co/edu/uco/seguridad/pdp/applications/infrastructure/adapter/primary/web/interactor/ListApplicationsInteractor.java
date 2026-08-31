package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response.ApplicationWebResponse;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.ListApplicationsRawRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;
import co.edu.uco.seguridad.shared.web.PageResponse;


/** Adaptador primario HTTP: lista el catálogo de aplicaciones del tenant autenticado. */
public interface ListApplicationsInteractor
        extends ReactiveOperation<ListApplicationsRawRequest, PageResponse<ApplicationWebResponse>> {
}
