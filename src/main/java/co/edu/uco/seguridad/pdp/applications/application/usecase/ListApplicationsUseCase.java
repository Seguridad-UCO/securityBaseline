package co.edu.uco.seguridad.pdp.applications.application.usecase;

import co.edu.uco.seguridad.pdp.applications.application.port.primary.dto.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.shared.contract.ReactiveStreamOperation;

/** Puerto primario: listar el catálogo de aplicaciones de un inquilino. */
public interface ListApplicationsUseCase extends ReactiveStreamOperation<TenantId, RegisteredApplicationResponse> {
}
