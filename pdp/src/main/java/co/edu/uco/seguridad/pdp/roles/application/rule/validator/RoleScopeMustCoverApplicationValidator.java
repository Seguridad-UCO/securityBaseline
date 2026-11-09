package co.edu.uco.seguridad.pdp.roles.application.rule.validator;

import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.RoleCoverageQuery;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/**
 * Contrato publicado a {@code assignments} (HU-005): el rol existe y su alcance cubre la aplicación
 * indicada (global cubre todo; tenant cubre su tenant; aplicación cubre solo la suya). Resuelve el
 * rol por id sin filtrar por tenant — a diferencia de {@code findByIdForTenant}, un rol GLOBAL debe
 * encontrarse igual.
 */
public interface RoleScopeMustCoverApplicationValidator extends ReactiveOperationWithoutResult<RoleCoverageQuery> {
}
