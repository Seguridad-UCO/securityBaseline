package co.edu.uco.seguridad.pdp.roles.domain.rule;

import co.edu.uco.seguridad.pdp.roles.domain.rule.model.ApplicationCoverage;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/** El alcance de un rol tiene que cubrir la aplicación indicada (INV-ASN-02, HU-005). */
public interface RoleScopeMustCoverApplicationRule extends OperationWithoutResult<ApplicationCoverage> {
}
