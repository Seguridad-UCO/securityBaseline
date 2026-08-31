package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.RegisterProtectedResourceRequest;
import co.edu.uco.seguridad.pdp.resources.domain.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedResourceRawRequest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
