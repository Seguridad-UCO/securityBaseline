package co.edu.uco.seguridad.pdp.authorization.application.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.InternalAccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AccessDecision;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Caso de uso puente del canal interno (HU-003, D4): resuelve el inquilino dueño de la aplicación
 * y delega en {@link AuthorizeUseCase}, que sigue siendo la única regla de decisión para los dos
 * canales (BFF e interno).
 */
public interface EvaluateInternalAccessUseCase extends ReactiveOperation<InternalAccessRequest, AccessDecision> {
}
