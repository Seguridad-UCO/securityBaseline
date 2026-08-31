package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.response.RegisteredProtectedResourceResponse;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.response.ProtectedResourceWebResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProtectedResourceResponseMapperTests {

    @Test
    void toResponse_unwraps_every_value_object_into_a_flat_web_response() {
        ResourceId id = new ResourceId(UUID.randomUUID());
        ApplicationId applicationId = new ApplicationId(UUID.randomUUID());
        Instant registeredAt = Instant.now();
        RegisteredProtectedResourceResponse response = new RegisteredProtectedResourceResponse(id, applicationId,
                new TenantId("universidad-uco"), new ResourcePath("/api/v1/grades"), HttpVerb.GET, registeredAt);

        ProtectedResourceWebResponse webResponse = ProtectedResourceResponseMapper.toResponse(response);

        assertThat(webResponse).isEqualTo(new ProtectedResourceWebResponse(id.value().toString(),
                applicationId.value().toString(), "universidad-uco", "/api/v1/grades", "GET", registeredAt));
    }

    @Test
    void toResponseList_maps_every_element_preserving_order() {
        ApplicationId applicationId = new ApplicationId(UUID.randomUUID());
        Instant registeredAt = Instant.now();
        RegisteredProtectedResourceResponse first = new RegisteredProtectedResourceResponse(
                new ResourceId(UUID.randomUUID()), applicationId, new TenantId("universidad-uco"),
                new ResourcePath("/a"), HttpVerb.GET, registeredAt);
        RegisteredProtectedResourceResponse second = new RegisteredProtectedResourceResponse(
                new ResourceId(UUID.randomUUID()), applicationId, new TenantId("universidad-uco"),
                new ResourcePath("/b"), HttpVerb.POST, registeredAt);

        List<ProtectedResourceWebResponse> webResponses = ProtectedResourceResponseMapper.toResponseList(
                List.of(first, second));

        assertThat(webResponses).extracting(ProtectedResourceWebResponse::path).containsExactly("/a", "/b");
    }
}
