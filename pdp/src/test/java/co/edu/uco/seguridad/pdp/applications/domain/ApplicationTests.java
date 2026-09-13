package co.edu.uco.seguridad.pdp.applications.domain;

import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationCredentialHash;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationTests {

    @Test
    void register_rejects_a_null_credential_hash() {
        assertThatThrownBy(() -> Application.register(new ApplicationId(UUID.randomUUID()),
                new TenantId("universidad-uco"), new ApplicationName("gestion-academica"), "",
                new ApplicationBaseUrl("https://example.com"), null, Instant.now()))
                .isInstanceOf(NullPointerException.class);
    }
}
