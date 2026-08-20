package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response.ApplicationWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutInput;

import java.util.List;

/** Adaptador primario HTTP: lista el catálogo de aplicaciones del tenant autenticado. */
public interface ListApplicationsInteractor extends ReactiveOperationWithoutInput<List<ApplicationWebResponse>> {
}
