package co.edu.uco.seguridad.pdp.identity.domain;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.domain.model.Email;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.time.Instant;
import java.util.Objects;

/**
 * Usuario propio de la plataforma. Nace del primer login exitoso vía un identity provider externo
 * (Keycloak) — ver {@link ExternalIdentity} para el vínculo con ese provider.
 */
public record SecurityUser(UserId id, TenantId tenantId, Email email, String name, Instant createdAt,
                           Instant lastLoginAt) {

    public SecurityUser {
        Objects.requireNonNull(id, RequiredArgumentMessages.USER_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(email, RequiredArgumentMessages.USER_EMAIL);
        Objects.requireNonNull(createdAt, RequiredArgumentMessages.REGISTERED_AT);
        Objects.requireNonNull(lastLoginAt, RequiredArgumentMessages.REGISTERED_AT);
        name = name == null ? "" : name.trim();
    }

    public static SecurityUser provision(UserId id, TenantId tenantId, Email email, String name, Instant now) {
        return new SecurityUser(id, tenantId, email, name, now, now);
    }

    public SecurityUser withLogin(String name, Instant now) {
        return new SecurityUser(id, tenantId, email, name, createdAt, now);
    }

    public SecurityUser withTenant(TenantId newTenantId) {
        return new SecurityUser(id, newTenantId, email, name, createdAt, lastLoginAt);
    }
}
