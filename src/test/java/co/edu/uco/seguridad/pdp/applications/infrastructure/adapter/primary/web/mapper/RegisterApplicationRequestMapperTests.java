package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.RegisterApplicationRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterApplicationRequestMapperTests {

    @Test
    void toRequest_maps_the_raw_fields_and_carries_the_tenant_from_the_token() {
        RegisterApplicationRawRequest raw = new RegisterApplicationRawRequest("Moodle", "  LMS institucional  ",
                "https://moodle.uco.edu.co");

        RegisterApplicationRequest request = RegisterApplicationRequestMapper.toRequest(raw, new TenantId("universidad-uco"));

        assertThat(request.tenantId()).isEqualTo(new TenantId("universidad-uco"));
        assertThat(request.name().value()).isEqualTo("Moodle");
        assertThat(request.description()).isEqualTo("LMS institucional");
        assertThat(request.baseUrl().value()).isEqualTo("https://moodle.uco.edu.co");
    }

    @Test
    void toRequest_normalizes_a_null_description_to_an_empty_string() {
        RegisterApplicationRawRequest raw = new RegisterApplicationRawRequest("Moodle", null, "https://moodle.uco.edu.co");

        RegisterApplicationRequest request = RegisterApplicationRequestMapper.toRequest(raw, new TenantId("universidad-uco"));

        assertThat(request.description()).isEmpty();
    }
}
