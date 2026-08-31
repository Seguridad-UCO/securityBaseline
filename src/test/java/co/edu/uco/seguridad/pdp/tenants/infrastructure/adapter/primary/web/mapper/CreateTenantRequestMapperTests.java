package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.primaryport.request.CreateTenantRequest;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.dto.request.raw.CreateTenantRawRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateTenantRequestMapperTests {

    @Test
    void toRequest_parses_every_raw_field() {
        CreateTenantRawRequest raw = new CreateTenantRawRequest("universidad-uco", "Universidad UCO");

        CreateTenantRequest request = CreateTenantRequestMapper.toRequest(raw);

        assertThat(request.id()).isEqualTo(new TenantId("universidad-uco"));
        assertThat(request.name().value()).isEqualTo("Universidad UCO");
    }
}
