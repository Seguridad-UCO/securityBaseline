package co.edu.uco.seguridad.pdp.identity.application.usecase;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.response.UserResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

import java.util.List;

/**
 * Puerto primario: listar los usuarios de un inquilino. Operación administrativa: quién puede
 * invocarla lo decide {@code authorization} (SU-01), nunca este caso de uso.
 */
public interface ListUsersUseCase extends ReactiveOperation<TenantId, List<UserResponse>> {
}
