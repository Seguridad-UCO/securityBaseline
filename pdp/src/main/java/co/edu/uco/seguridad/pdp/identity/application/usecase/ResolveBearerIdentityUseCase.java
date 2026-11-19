package co.edu.uco.seguridad.pdp.identity.application.usecase;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.request.ResolveBearerIdentityRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Puerto primario (HU-038): resuelve el {@code UserId} local de un sujeto autenticado por Bearer y,
 * si nunca se vio, lo aprovisiona de forma idempotente y atómica en el tenant del claim. Falla
 * (error de dominio) si el tenant no existe o no está activo, o si el correo ya es de otro usuario;
 * en ese caso no se crea nada.
 */
public interface ResolveBearerIdentityUseCase extends ReactiveOperation<ResolveBearerIdentityRequest, UserId> {
}
