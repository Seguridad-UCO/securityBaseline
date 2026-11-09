package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedResourceRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.RegisterProtectedResourceRequest;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-017: movido desde {@code resources}, mismo caso — ver PLAN-HU-017.md §7.
 */
class RegisterProtectedResourceRequestMapperTests {

    @Test
    void toRequest_parses_every_raw_field_and_carries_the_tenant_from_the_token() {
        UUID applicationId = UUID.randomUUID();
        RegisterProtectedResourceRawRequest raw = new RegisterProtectedResourceRawRequest(applicationId.toString(),
                "/api/v1/grades", "get");

        RegisterProtectedResourceRequest request = RegisterProtectedResourceRequestMapper.toRequest(raw,
                new TenantId("universidad-uco"));

        assertThat(request.tenantId()).isEqualTo(new TenantId("universidad-uco"));
        assertThat(request.applicationId()).isEqualTo(new ApplicationId(applicationId));
        assertThat(request.path().value()).isEqualTo("/api/v1/grades");
        assertThat(request.method()).isEqualTo(HttpVerb.GET);
    }
}
