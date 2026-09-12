package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.RevokeAssignmentRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RevokeAssignmentRequestMapperTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final String ASSIGNMENT_ID = UUID.randomUUID().toString();

    @Test
    void rejects_an_assignment_id_that_is_not_a_valid_identifier() {
        assertThatThrownBy(() -> map(new RevokeAssignmentRawRequest("not-a-uuid")))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("assignmentId");
    }

    @Test
    void builds_the_request_with_the_tenant_from_the_principal() {
        RevokeAssignmentRequest request = map(new RevokeAssignmentRawRequest(ASSIGNMENT_ID));

        assertThat(request.assignmentId()).isEqualTo(AssignmentId.of(ASSIGNMENT_ID));
        assertThat(request.tenantId()).isEqualTo(TENANT);
    }

    private static RevokeAssignmentRequest map(RevokeAssignmentRawRequest raw) {
        return RevokeAssignmentRequestMapper.toRequest(raw, TENANT);
    }
}
