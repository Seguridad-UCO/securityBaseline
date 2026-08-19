package co.edu.uco.seguridad.pdp.resources.application.usecase;

import co.edu.uco.seguridad.pdp.commons.ResultPage;
import co.edu.uco.seguridad.pdp.resources.application.port.primary.dto.request.SearchProtectedApplicationsRequest;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Puerto primario: consultar el catálogo por criterios.
 * Devuelve dominio; el interactor proyecta al DTO de salida.
 */
public interface SearchProtectedApplicationsUseCase
        extends ReactiveOperation<SearchProtectedApplicationsRequest, ResultPage<ProtectedResource>> {
}
