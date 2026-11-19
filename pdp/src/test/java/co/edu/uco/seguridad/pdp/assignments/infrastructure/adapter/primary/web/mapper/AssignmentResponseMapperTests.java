package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.AssignmentWebResponse;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AssignmentResponseMapperTests {

    private static final AssignmentId ID = new AssignmentId(UUID.randomUUID());
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final RoleId ROLE = new RoleId(UUID.randomUUID());
    private static final Instant VALID_FROM = Instant.parse("2026-09-12T00:00:00Z");

    @Test
    void carries_the_end_as_a_string_when_present() {
        Instant validUntil = VALID_FROM.plusSeconds(3600);
        AssignmentResponse response = new AssignmentResponse(ID, USER, TENANT, APPLICATION, ROLE, VALID_FROM,
                Optional.of(validUntil));

        AssignmentWebResponse web = AssignmentResponseMapper.toResponse(response);

        assertThat(web.validUntil()).isEqualTo(validUntil.toString());
    }

    @Test
    void leaves_the_end_null_while_still_active() {
        AssignmentResponse response = new AssignmentResponse(ID, USER, TENANT, APPLICATION, ROLE, VALID_FROM, Optional.empty());

        AssignmentWebResponse web = AssignmentResponseMapper.toResponse(response);

        assertThat(web.validUntil()).isNull();
    }
}
