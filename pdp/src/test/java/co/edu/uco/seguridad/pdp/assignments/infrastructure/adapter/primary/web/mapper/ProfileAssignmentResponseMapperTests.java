package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ProfileAssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.ProfileAssignmentWebResponse;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileAssignmentResponseMapperTests {

    private static final ProfileAssignmentId ID = new ProfileAssignmentId(UUID.randomUUID());
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final ProfileId PROFILE = ProfileId.of(UUID.randomUUID().toString());
    private static final AssignmentId GENERATED = new AssignmentId(UUID.randomUUID());
    private static final Instant VALID_FROM = Instant.parse("2026-09-12T00:00:00Z");

    @Test
    void carries_the_end_as_a_string_when_present() {
        Instant validUntil = VALID_FROM.plusSeconds(3600);
        ProfileAssignmentResponse response = new ProfileAssignmentResponse(ID, USER, TENANT, APPLICATION, PROFILE,
                Set.of(GENERATED), VALID_FROM, Optional.of(validUntil));

        ProfileAssignmentWebResponse web = ProfileAssignmentResponseMapper.toResponse(response);

        assertThat(web.validUntil()).isEqualTo(validUntil.toString());
        assertThat(web.generatedAssignmentIds()).containsExactly(GENERATED.value().toString());
    }

    @Test
    void leaves_the_end_null_while_still_active() {
        ProfileAssignmentResponse response = new ProfileAssignmentResponse(ID, USER, TENANT, APPLICATION, PROFILE,
                Set.of(), VALID_FROM, Optional.empty());

        ProfileAssignmentWebResponse web = ProfileAssignmentResponseMapper.toResponse(response);

        assertThat(web.validUntil()).isNull();
        assertThat(web.generatedAssignmentIds()).isEmpty();
    }
}
