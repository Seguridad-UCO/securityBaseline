package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.DefineRoleRequest;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;
import java.util.Optional;

/**
 * Entrada de {@code AdministerRoleDefinitionUseCase} (HU-016): la solicitud de definición de rol,
 * más la administración a gatear si el rol es de alcance {@code APPLICATION}. {@code administration}
 * vacío significa "esta escritura no requiere administración" (rol {@code TENANT}) — nunca "no se
 * pudo resolver": si el rol no existe la resolución previa ya rechazó antes de llegar aquí.
 *
 * <p>{@code Optional} como componente es el mismo patrón ya aceptado en {@code RoleScope}: este
 * record no se serializa nunca, y un campo anulable dejaría {@code equals} operando sobre un tipo
 * distinto al que anuncia el accessor.
 */
public record AdministerRoleDefinitionRequest(Optional<AdministrationRequest> administration, DefineRoleRequest role) {

    public AdministerRoleDefinitionRequest {
        Objects.requireNonNull(administration, RequiredArgumentMessages.ADMINISTRATION);
        Objects.requireNonNull(role, RequiredArgumentMessages.DEFINE_ROLE_REQUEST);
    }
}
