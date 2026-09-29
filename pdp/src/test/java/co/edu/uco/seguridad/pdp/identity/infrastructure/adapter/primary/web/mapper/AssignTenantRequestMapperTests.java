package co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.request.AssignTenantRequest;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.dto.request.raw.AssignTenantRawRequest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AssignTenantRequestMapperTests {

    @Test
    void toRequest_parses_every_raw_field() {
        UUID userId = UUID.randomUUID();
        AssignTenantRawRequest raw = new AssignTenantRawRequest(userId.toString(), "otra-universidad");

        AssignTenantRequest request = AssignTenantRequestMapper.toRequest(raw);

        assertThat(request.userId()).isEqualTo(new UserId(userId));
        assertThat(request.tenantId()).isEqualTo(new TenantId("otra-universidad"));
    }
}
