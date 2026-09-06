package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.secondary.persistence.mapper;

import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.secondary.persistence.entity.ApplicationEntity;
import co.edu.uco.seguridad.pdp.commons.exception.InvalidTenantIdException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationPersistenceMapperTests {

    private static final String ID = UUID.randomUUID().toString();

    @Test
    void rebuilds_the_aggregate_from_the_row() {
        Application application = ApplicationPersistenceMapper.toDomain(entity("gestion-academica"));

        assertThat(application.id().value()).hasToString(ID);
        assertThat(application.tenantId().value()).isEqualTo("universidad-uco");
        assertThat(application.name().value()).isEqualTo("gestion-academica");
        assertThat(application.baseUrl().value()).isEqualTo("https://example.com");
        assertThat(application.registeredAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void rejects_a_row_whose_tenant_is_malformed() {
        ApplicationEntity corrupted = new ApplicationEntity(ID, "tenant con espacios", "gestion-academica", "",
                "https://example.com", "2026-01-01T00:00:00Z");

        assertThatThrownBy(() -> ApplicationPersistenceMapper.toDomain(corrupted))
                .isInstanceOf(InvalidTenantIdException.class);
    }

    private static ApplicationEntity entity(String name) {
        return new ApplicationEntity(ID, "universidad-uco", name, "", "https://example.com",
                "2026-01-01T00:00:00Z");
    }
}
