package co.edu.uco.seguridad.pdp.roles.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.RoleNameInScopeQuery;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Resuelve el identificador de un rol por nombre+alcance, si existe (HU-015: backfill del primer
 * administrador). Vacío si no hay ninguno. Publicado en vez de dejar que {@code assignments} consulte
 * {@code RoleRepository} directamente — mismo patrón que {@code ApplicationOwnerLookupValidator}: una
 * decisión sobre otro módulo se consume como validador, nunca consultando su repositorio
 * (sb-arquitectura, regla invariante 11).
 */
public interface RoleLookupByNameInScopeValidator extends ReactiveOperation<RoleNameInScopeQuery, RoleId> {
}
