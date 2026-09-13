package co.edu.uco.seguridad.pdp.assignments.application.primaryport.request;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada de {@code RegisterApplicationWithFirstAdministratorUseCase} (HU-015). Envuelve el
 * {@code RegisterApplicationRequest} de {@code applications :: dto} (que no cambia — sigue sin saber
 * nada de administradores) junto con el {@code UserId} del registrador, que el interactor resuelve
 * del principal autenticado y que este caso de uso necesita para la asignación del rol {@code ADMIN}.
 */
public record RegisterApplicationWithFirstAdministratorRequest(
        RegisterApplicationRequest application, UserId registrarUserId) {

    public RegisterApplicationWithFirstAdministratorRequest {
        Objects.requireNonNull(application, RequiredArgumentMessages.DTO);
        Objects.requireNonNull(registrarUserId, RequiredArgumentMessages.USER_ID);
    }
}
