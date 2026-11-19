package co.edu.uco.seguridad.pdp.roles.domain.model;

import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.domain.exception.InvalidRoleScopeException;

import java.util.Objects;
import java.util.Optional;

/**
 * Alcance de un rol: el nivel más los identificadores que ese nivel exige, y solo esos.
 * {@code GLOBAL} sin ids; {@code TENANT} solo inquilino; {@code APPLICATION} inquilino y aplicación.
 *
 * <p>{@code Optional} como componente es una decisión consciente con precedente en
 * {@code ApplicationCriteria}: este objeto no se serializa nunca (la persistencia y la respuesta
 * web lo aplanan), y un campo anulable dejaría {@code equals} operando sobre un tipo distinto al
 * que anuncia el accessor. No lo corrijas a un campo anulable.
 */
public record RoleScope(RoleScopeLevel level, Optional<TenantId> tenantId, Optional<ApplicationId> applicationId) {

    public RoleScope {
        Objects.requireNonNull(level, ValueObjectMessages.RoleScope.UNSUPPORTED_LEVEL);
        Objects.requireNonNull(tenantId, ValueObjectMessages.RoleScope.INCOHERENT);
        Objects.requireNonNull(applicationId, ValueObjectMessages.RoleScope.INCOHERENT);
        if (tenantId.isPresent() != level.requiresTenant() || applicationId.isPresent() != level.requiresApplication()) {
            throw new InvalidRoleScopeException(ValueObjectMessages.RoleScope.INCOHERENT);
        }
    }

    public static RoleScope global() {
        return new RoleScope(RoleScopeLevel.GLOBAL, Optional.empty(), Optional.empty());
    }

    public static RoleScope ofTenant(TenantId tenantId) {
        return new RoleScope(RoleScopeLevel.TENANT, Optional.of(tenantId), Optional.empty());
    }

    public static RoleScope ofApplication(TenantId tenantId, ApplicationId applicationId) {
        return new RoleScope(RoleScopeLevel.APPLICATION, Optional.of(tenantId), Optional.of(applicationId));
    }

    public boolean isGlobal() {
        return level == RoleScopeLevel.GLOBAL;
    }
}
