package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.mapper;

import co.edu.uco.seguridad.pdp.assignments.domain.Assignment;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.entity.AssignmentEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AssignmentPersistenceMapperTests {

    private static final String ID = UUID.randomUUID().toString();
    private static final String USER_ID = UUID.randomUUID().toString();
    private static final String TENANT = "universidad-uco";
    private static final String APPLICATION_ID = UUID.randomUUID().toString();
    private static final String ROLE_ID = UUID.randomUUID().toString();
    private static final String VALID_FROM = "2026-09-12T00:00:00Z";

    @Test
    void a_null_valid_until_becomes_a_validity_without_end() {
        AssignmentEntity entity = new AssignmentEntity(ID, USER_ID, TENANT, APPLICATION_ID, ROLE_ID, VALID_FROM, null);

        Assignment assignment = AssignmentPersistenceMapper.toDomain(entity);

        assertThat(assignment.validity().validUntil()).isEmpty();
    }

    @Test
    void a_present_valid_until_becomes_a_validity_with_end() {
        String validUntil = "2026-10-12T00:00:00Z";
        AssignmentEntity entity = new AssignmentEntity(ID, USER_ID, TENANT, APPLICATION_ID, ROLE_ID, VALID_FROM, validUntil);

        Assignment assignment = AssignmentPersistenceMapper.toDomain(entity);

        assertThat(assignment.validity().validUntil()).contains(Instant.parse(validUntil));
    }
}
