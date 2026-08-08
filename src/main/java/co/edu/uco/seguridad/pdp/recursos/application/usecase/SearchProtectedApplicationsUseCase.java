package co.edu.uco.seguridad.pdp.recursos.application.usecase;

import co.edu.uco.seguridad.pdp.commons.ResultPage;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.SearchProtectedApplicationsRequest;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.response.ProtectedApplicationResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Puerto primario: consultar el catálogo por criterios.
 *
 * <p>Devuelve {@code Mono<ResultPage<...>>} porque el recuento total y la ventana son parte de la respuesta.</p>
 */
public interface SearchProtectedApplicationsUseCase
        extends ReactiveOperation<SearchProtectedApplicationsRequest, ResultPage<ProtectedApplicationResponse>> {
}
