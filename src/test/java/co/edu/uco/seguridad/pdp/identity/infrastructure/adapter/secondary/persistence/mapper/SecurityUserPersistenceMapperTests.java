package co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.secondary.persistence.mapper;

import co.edu.uco.seguridad.pdp.identity.domain.ExternalIdentity;
import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import co.edu.uco.seguridad.pdp.identity.domain.exception.InvalidEmailException;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.secondary.persistence.entity.ExternalIdentityEntity;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.secondary.persistence.entity.SecurityUserEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityUserPersistenceMapperTests {

    private static final String ID = UUID.randomUUID().toString();

    @Test
    void rebuilds_the_user_from_the_row() {
        SecurityUser user = SecurityUserPersistenceMapper.toDomain(userEntity("david@uco.edu.co"));

        assertThat(user.id().value()).hasToString(ID);
        assertThat(user.email().value()).isEqualTo("david@uco.edu.co");
        assertThat(user.tenantId().value()).isEqualTo("universidad-uco");
        assertThat(user.createdAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void rejects_a_row_whose_email_is_malformed() {
        assertThatThrownBy(() -> SecurityUserPersistenceMapper.toDomain(userEntity("no-es-un-correo")))
                .isInstanceOf(InvalidEmailException.class);
    }

    @Test
    void rebuilds_the_external_identity_from_the_row() {
        ExternalIdentity identity = SecurityUserPersistenceMapper.toDomain(
                new ExternalIdentityEntity(ID, "https://keycloak.local", "subject-1", "keycloak"));

        assertThat(identity.userId().value()).hasToString(ID);
        assertThat(identity.issuer()).isEqualTo("https://keycloak.local");
        assertThat(identity.provider()).isEqualTo("keycloak");
    }

    private static SecurityUserEntity userEntity(String email) {
        return new SecurityUserEntity(ID, "universidad-uco", email, "David", "2026-01-01T00:00:00Z",
                "2026-01-02T00:00:00Z");
    }
}
