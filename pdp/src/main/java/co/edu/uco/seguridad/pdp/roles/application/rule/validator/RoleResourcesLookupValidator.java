package co.edu.uco.seguridad.pdp.roles.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

import java.util.Set;

/**
 * Proyección publicada de los grants actuales de roles efectivos.
 */
public interface RoleResourcesLookupValidator extends ReactiveOperation<Set<RoleId>, Set<ResourceId>> {
}
