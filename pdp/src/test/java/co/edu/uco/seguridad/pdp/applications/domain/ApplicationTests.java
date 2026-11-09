package co.edu.uco.seguridad.pdp.applications.domain;

import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationCredentialHash;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationTests {

    @Test
    void register_rejects_a_null_credential_hash() {
        assertThatThrownBy(() -> Application.register(new ApplicationId(UUID.randomUUID()),
                new TenantId("universidad-uco"), new ApplicationName("gestion-academica"), "",
                new ApplicationBaseUrl("https://example.com"), null, Instant.now()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void withCredentialHash_replaces_only_the_hash_keeping_the_rest_of_the_identity() {
        Instant registeredAt = Instant.parse("2026-01-01T00:00:00Z");
        Application original = Application.register(new ApplicationId(UUID.randomUUID()),
                new TenantId("universidad-uco"), new ApplicationName("gestion-academica"), "Sistema académico",
                new ApplicationBaseUrl("https://example.com"), new ApplicationCredentialHash("hash-viejo"),
                registeredAt);

        Application rotated = original.withCredentialHash(new ApplicationCredentialHash("hash-nuevo"));

        assertThat(rotated.id()).isEqualTo(original.id());
        assertThat(rotated.tenantId()).isEqualTo(original.tenantId());
        assertThat(rotated.name()).isEqualTo(original.name());
        assertThat(rotated.description()).isEqualTo(original.description());
        assertThat(rotated.baseUrl()).isEqualTo(original.baseUrl());
        assertThat(rotated.registeredAt()).isEqualTo(original.registeredAt());
        assertThat(rotated.credentialHash()).isEqualTo(new ApplicationCredentialHash("hash-nuevo"));
    }
}
