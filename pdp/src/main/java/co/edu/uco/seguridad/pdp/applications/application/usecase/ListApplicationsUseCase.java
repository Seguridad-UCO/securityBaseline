package co.edu.uco.seguridad.pdp.applications.application.usecase;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ListApplicationsRequest;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/** Puerto primario: listar el catálogo de aplicaciones de un inquilino. */
public interface ListApplicationsUseCase
        extends ReactiveOperation<ListApplicationsRequest, ResultPage<RegisteredApplicationResponse>> {
}
