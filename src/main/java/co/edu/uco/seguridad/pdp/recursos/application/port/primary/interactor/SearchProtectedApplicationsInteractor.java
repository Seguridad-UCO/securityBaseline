package co.edu.uco.seguridad.pdp.recursos.application.port.primary.interactor;

import co.edu.uco.seguridad.pdp.commons.ResultPage;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.response.ProtectedApplicationResponse;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.SearchProtectedApplicationsRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Interactor del puerto primario: punto de entrada de aplicación para consultar el catálogo.
 *
 * <p>Recibe y devuelve DTOs de aplicación (no DTOs web). El adaptador primario mapea la entrada/salida
 * de transporte; este contrato permanece independiente del canal.</p>
 */
public interface SearchProtectedApplicationsInteractor
        extends ReactiveOperation<SearchProtectedApplicationsRequest, ResultPage<ProtectedApplicationResponse>> {
}
