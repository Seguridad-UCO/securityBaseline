package co.edu.uco.seguridad.pdp.identity.application.rule;

import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import co.edu.uco.seguridad.pdp.identity.domain.UserId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * El usuario referenciado existe. Consulta el repositorio, así que es reactiva, y devuelve el
 * agregado para que el caso de uso no lo vuelva a buscar.
 */
public interface UserMustExistRule extends ReactiveOperation<UserId, SecurityUser> {
}
