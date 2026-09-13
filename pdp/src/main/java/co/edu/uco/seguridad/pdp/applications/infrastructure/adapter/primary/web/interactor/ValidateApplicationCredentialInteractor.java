package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.ValidateApplicationCredentialRawRequest;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response.ApplicationCredentialValidationWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Adaptador primario HTTP del canal interno de validación (HU-013). Sin lectura de
 * {@code SecurityContext} ni del JWT de evidencia — no hay "quién pregunta", solo "esta aplicación
 * con este secreto, ¿es válida?" (ver Hallazgo 1 del plan).
 */
public interface ValidateApplicationCredentialInteractor
        extends ReactiveOperation<ValidateApplicationCredentialRawRequest, ApplicationCredentialValidationWebResponse> {
}
