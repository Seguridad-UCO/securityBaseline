package co.edu.uco.seguridad.pdp.identity.application.usecase;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.response.UserResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/** Lectura pública mínima de una persona para composiciones intermodulares. */
public interface FindUserByIdUseCase extends ReactiveOperation<UserId, UserResponse> {
}
