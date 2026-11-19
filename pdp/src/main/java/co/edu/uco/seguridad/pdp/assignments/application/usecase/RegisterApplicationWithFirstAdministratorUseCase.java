package co.edu.uco.seguridad.pdp.assignments.application.usecase;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.ApplicationRegistrationResponse;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RegisterApplicationWithFirstAdministratorRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Orquesta HU-015: registra la aplicación ({@code applications :: usecase}), define su rol
 * {@code ADMIN} de alcance {@code APPLICATION} ({@code roles :: usecase}) y se lo asigna al
 * registrador (este mismo módulo) — en ese orden, en el mismo request. Vive en {@code assignments}
 * porque es el único módulo que ya depende a la vez de {@code applications} y de {@code roles}
 * (ver PLAN-HU-015.md §0).
 */
public interface RegisterApplicationWithFirstAdministratorUseCase
        extends ReactiveOperation<RegisterApplicationWithFirstAdministratorRequest, ApplicationRegistrationResponse> {
}
