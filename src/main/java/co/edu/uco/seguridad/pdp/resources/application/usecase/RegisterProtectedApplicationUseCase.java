package co.edu.uco.seguridad.pdp.resources.application.usecase;

import co.edu.uco.seguridad.pdp.resources.application.port.primary.dto.request.RegisterProtectedApplicationRequest;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Puerto primario: registrar una aplicación protegida y su primer recurso como unidad de trabajo.
 * Devuelve el agregado de dominio; el interactor proyecta al DTO de salida.
 */
public interface RegisterProtectedApplicationUseCase
        extends ReactiveOperation<RegisterProtectedApplicationRequest, ProtectedResource> {
}
