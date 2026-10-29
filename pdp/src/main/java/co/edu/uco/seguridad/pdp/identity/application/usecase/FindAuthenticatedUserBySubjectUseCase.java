package co.edu.uco.seguridad.pdp.identity.application.usecase;

import co.edu.uco.seguridad.pdp.identity.application.primaryport.response.AuthenticatedUserResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/** Busca la identidad local vinculada al {@code sub} de una sesión o JWT. */
public interface FindAuthenticatedUserBySubjectUseCase extends ReactiveOperation<String, AuthenticatedUserResponse> {
}
