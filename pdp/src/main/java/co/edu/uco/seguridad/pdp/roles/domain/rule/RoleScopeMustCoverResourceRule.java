package co.edu.uco.seguridad.pdp.roles.domain.rule;

import co.edu.uco.seguridad.pdp.roles.domain.rule.model.ResourceCoverage;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/**
 * Regla R5 (INV-DAT-01): global cubre todo; inquilino cubre sus aplicaciones; aplicación cubre solo la suya.
 */
public interface RoleScopeMustCoverResourceRule extends OperationWithoutResult<ResourceCoverage> {
}
