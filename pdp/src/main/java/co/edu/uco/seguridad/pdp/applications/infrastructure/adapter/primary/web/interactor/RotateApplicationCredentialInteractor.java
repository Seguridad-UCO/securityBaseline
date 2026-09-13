package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.RotateApplicationCredentialRawRequest;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response.ApplicationRegisteredWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Adaptador primario HTTP: recibe el {@code applicationId} del path, mapea con el tenant del
 * principal, ejecuta el caso de uso y proyecta la respuesta HTTP (HU-014).
 */
public interface RotateApplicationCredentialInteractor
        extends ReactiveOperation<RotateApplicationCredentialRawRequest, ApplicationRegisteredWebResponse> {
}
