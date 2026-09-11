package co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.response.UserResponse;
import co.edu.uco.seguridad.pdp.identity.domain.model.UserId;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.dto.response.UserWebResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserResponseMapperTests {

    @Test
    void toResponse_unwraps_every_value_object_into_a_flat_web_response() {
        UserId id = new UserId(UUID.randomUUID());
        Instant createdAt = Instant.now();
        Instant lastLoginAt = Instant.now();
        UserResponse response = new UserResponse(id, "david@uco.edu", "David", "google",
                new TenantId("universidad-uco"), createdAt, lastLoginAt);

        UserWebResponse webResponse = UserResponseMapper.toResponse(response);

        assertThat(webResponse).isEqualTo(new UserWebResponse(id.value().toString(), "david@uco.edu", "David",
                "google", "universidad-uco", createdAt, lastLoginAt));
    }

    @Test
    void toResponseList_maps_every_element_preserving_order() {
        Instant now = Instant.now();
        UserResponse first = new UserResponse(new UserId(UUID.randomUUID()), "a@uco.edu", "A", "google",
                new TenantId("universidad-uco"), now, now);
        UserResponse second = new UserResponse(new UserId(UUID.randomUUID()), "b@uco.edu", "B", "google",
                new TenantId("universidad-uco"), now, now);

        List<UserWebResponse> webResponses = UserResponseMapper.toResponseList(List.of(first, second));

        assertThat(webResponses).extracting(UserWebResponse::email).containsExactly("a@uco.edu", "b@uco.edu");
    }
}
