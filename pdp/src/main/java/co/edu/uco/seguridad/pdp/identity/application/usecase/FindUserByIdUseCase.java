package co.edu.uco.seguridad.pdp.identity.application.usecase;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.response.UserResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Puerto primario: leer un usuario por identificador. Vacío si no existe; el filtrado por inquilino
 * es de quien consume.
 */
public interface FindUserByIdUseCase extends ReactiveOperation<UserId, UserResponse> {
}
