package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response.ApplicationWebResponse;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationResponseMapperTests {

    @Test
    void toResponse_unwraps_every_value_object_into_a_flat_web_response() {
        ApplicationId id = new ApplicationId(UUID.randomUUID());
        Instant registeredAt = Instant.now();
        RegisteredApplicationResponse response = new RegisteredApplicationResponse(id, new TenantId("universidad-uco"),
                new ApplicationName("Moodle"), "LMS institucional", new ApplicationBaseUrl("https://moodle.uco.edu.co"),
                registeredAt);

        ApplicationWebResponse webResponse = ApplicationResponseMapper.toResponse(response);

        assertThat(webResponse).isEqualTo(new ApplicationWebResponse(id.value().toString(), "universidad-uco",
                "Moodle", "LMS institucional", "https://moodle.uco.edu.co", registeredAt));
    }

    @Test
    void toResponseList_maps_every_element_preserving_order() {
        ApplicationId first = new ApplicationId(UUID.randomUUID());
        ApplicationId second = new ApplicationId(UUID.randomUUID());
        Instant registeredAt = Instant.now();
        RegisteredApplicationResponse firstResponse = new RegisteredApplicationResponse(first,
                new TenantId("universidad-uco"), new ApplicationName("Moodle"), "", new ApplicationBaseUrl("https://a.uco.edu.co"),
                registeredAt);
        RegisteredApplicationResponse secondResponse = new RegisteredApplicationResponse(second,
                new TenantId("universidad-uco"), new ApplicationName("Canvas"), "", new ApplicationBaseUrl("https://b.uco.edu.co"),
                registeredAt);

        List<ApplicationWebResponse> webResponses = ApplicationResponseMapper.toResponseList(
                List.of(firstResponse, secondResponse));

        assertThat(webResponses).extracting(ApplicationWebResponse::id)
                .containsExactly(first.value().toString(), second.value().toString());
    }
}
