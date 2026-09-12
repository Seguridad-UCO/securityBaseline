package co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.secondary.persistence.mapper;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.identity.domain.model.Email;
import co.edu.uco.seguridad.pdp.identity.domain.model.ExternalIdentity;
import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.secondary.persistence.entity.ExternalIdentityEntity;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.secondary.persistence.entity.SecurityUserEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Traduce las filas de persistencia a los agregados. Los value objects validan aquí: una fila
 * corrupta se rechaza al reconstruirla, no más adelante.
 *
 * <p>Este slice persiste dos agregados en el mismo adaptador, así que el mapper expone una
 * conversión por cada uno en vez de un único {@code toDomain} ambiguo.
 */
public final class SecurityUserPersistenceMapper {

    private SecurityUserPersistenceMapper() {
    }

    public static SecurityUser toDomain(SecurityUserEntity entity) {
        return new SecurityUser(
                new UserId(UUID.fromString(entity.id())),
                new TenantId(entity.tenantId()),
                new Email(entity.email()),
                entity.name(),
                Instant.parse(entity.createdAt()),
                Instant.parse(entity.lastLoginAt()));
    }

    public static ExternalIdentity toDomain(ExternalIdentityEntity entity) {
        return new ExternalIdentity(
                new UserId(UUID.fromString(entity.userId())),
                entity.issuer(),
                entity.subject(),
                entity.provider());
    }
}
