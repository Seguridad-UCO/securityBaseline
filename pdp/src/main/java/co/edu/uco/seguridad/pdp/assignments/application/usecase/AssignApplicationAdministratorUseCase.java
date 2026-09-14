package co.edu.uco.seguridad.pdp.assignments.application.usecase;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignApplicationAdministratorRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Backfill manual (HU-015) para aplicaciones registradas antes de que existiera el alta automática
 * del primer administrador: encuentra o crea el rol {@code ADMIN} de esa aplicación y lo asigna al
 * {@code userId} recibido. Expuesto en {@code /internal/v1/applications/{applicationId}/administrators}
 * — hereda la cadena mTLS del canal interno, no requiere seguridad propia.
 */
public interface AssignApplicationAdministratorUseCase
        extends ReactiveOperation<AssignApplicationAdministratorRequest, AssignmentResponse> {
}
