package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.UserWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutInput;

import java.util.List;

/**
 * Adaptador primario HTTP: lista los usuarios del inquilino del principal, si el principal es actor
 * de inquilino (SU-01). El inquilino sale del principal, nunca de la petición.
 */
public interface AdministerUserListingInteractor extends ReactiveOperationWithoutInput<List<UserWebResponse>> {
}
