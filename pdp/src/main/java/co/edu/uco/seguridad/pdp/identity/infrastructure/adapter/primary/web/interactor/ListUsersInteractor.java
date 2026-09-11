package co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.dto.response.UserWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutInput;

import java.util.List;

/** Adaptador primario HTTP: lista el catálogo completo de usuarios. Sin entrada: operación administrativa. */
public interface ListUsersInteractor extends ReactiveOperationWithoutInput<List<UserWebResponse>> {
}
