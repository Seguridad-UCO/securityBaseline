package co.edu.uco.seguridad.pdp.authorization.application.rule.validator;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveActiveRolesRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

import java.util.Set;

/**
 * Orquesta {@code assignments.ResolveActiveRolesUseCase} y {@code roles.RoleNamesLookupValidator}
 * (HU-008): dado un usuario y una aplicación, los nombres de sus roles activos, listos para
 * {@code subject.roles} de OPA. Ninguno de los dos módulos publica esta combinación porque ninguno
 * es dueño de ella — es {@code authorization} quien la necesita.
 */
public interface ActiveRoleNamesLookupValidator extends ReactiveOperation<ResolveActiveRolesRequest, Set<String>> {
}
