package co.edu.uco.seguridad.pdp.identity.application.usecase;

import co.edu.uco.seguridad.pdp.identity.application.primaryport.response.UserResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutInput;

import java.util.List;

/**
 * Puerto primario: listar el catálogo completo de usuarios. Operación administrativa.
 */
public interface ListUsersUseCase extends ReactiveOperationWithoutInput<List<UserResponse>> {
}
