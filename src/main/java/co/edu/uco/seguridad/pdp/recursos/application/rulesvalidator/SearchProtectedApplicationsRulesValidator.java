package co.edu.uco.seguridad.pdp.recursos.application.rulesvalidator;

import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.SearchProtectedApplicationsRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/**
 * Coordina las reglas que protegen las lecturas del catálogo.
 *
 * <p>La ventana no necesita regla: {@code PageWindow} no puede construirse fuera de sus límites, así que
 * el criterio 18 se aplica por el tipo en lugar de por una verificación que podría olvidarse.</p>
 */
public interface SearchProtectedApplicationsRulesValidator
        extends ReactiveOperationWithoutResult<SearchProtectedApplicationsRequest> {
}
