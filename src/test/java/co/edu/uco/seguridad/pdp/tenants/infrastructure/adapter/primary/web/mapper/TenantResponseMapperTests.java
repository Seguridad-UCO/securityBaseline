package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.port.primary.dto.response.TenantResponse;
import co.edu.uco.seguridad.pdp.tenants.domain.TenantName;
import co.edu.uco.seguridad.pdp.tenants.domain.TenantStatus;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.dto.response.TenantWebResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TenantResponseMapperTests {

    @Test
    void toResponse_unwraps_every_value_object_into_a_flat_web_response() {
        TenantResponse tenant = new TenantResponse(new TenantId("universidad-uco"), new TenantName("Universidad UCO"),
                TenantStatus.ACTIVE);

        TenantWebResponse webResponse = TenantResponseMapper.toResponse(tenant);

        assertThat(webResponse).isEqualTo(new TenantWebResponse("universidad-uco", "Universidad UCO", "ACTIVE"));
    }

    @Test
    void toResponseList_maps_every_element_preserving_order() {
        TenantResponse first = new TenantResponse(new TenantId("uno"), new TenantName("Uno"), TenantStatus.ACTIVE);
        TenantResponse second = new TenantResponse(new TenantId("dos"), new TenantName("Dos"), TenantStatus.SUSPENDED);

        List<TenantWebResponse> webResponses = TenantResponseMapper.toResponseList(List.of(first, second));

        assertThat(webResponses).extracting(TenantWebResponse::code).containsExactly("uno", "dos");
    }
}
