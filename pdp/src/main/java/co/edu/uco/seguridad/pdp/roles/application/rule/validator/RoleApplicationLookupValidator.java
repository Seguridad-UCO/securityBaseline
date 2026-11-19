package co.edu.uco.seguridad.pdp.roles.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.RoleOwnershipQuery;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

import java.util.Optional;

/**
 * Resuelve a qué aplicación pertenece un rol, si a alguna (HU-016). Vacío para alcance
 * {@code TENANT} — el único otro alcance que este canal admite hoy ({@code GLOBAL} se rechaza en
 * el mapper desde HU-004). Rechaza con {@code RoleNotFoundException} si el rol no existe para ese
 * inquilino, mismo criterio que {@link RoleMustExistForTenantValidator}.
 */
public interface RoleApplicationLookupValidator
        extends ReactiveOperation<RoleOwnershipQuery, Optional<ApplicationId>> {
}
