package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.mapper;

import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignment;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.entity.ProfileAssignmentEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Espejo de AssignmentPersistenceMapperTests.
 */
class ProfileAssignmentPersistenceMapperTests {

    private static final String ID = UUID.randomUUID().toString();
    private static final String USER_ID = UUID.randomUUID().toString();
    private static final String TENANT = "universidad-uco";
    private static final String APPLICATION_ID = UUID.randomUUID().toString();
    private static final String PROFILE_ID = UUID.randomUUID().toString();
    private static final String GENERATED_ID = UUID.randomUUID().toString();
    private static final String VALID_FROM = "2026-09-12T00:00:00Z";

    @Test
    void a_null_valid_until_becomes_a_validity_without_end() {
        ProfileAssignmentEntity entity = new ProfileAssignmentEntity(ID, USER_ID, TENANT, APPLICATION_ID, PROFILE_ID,
                List.of(GENERATED_ID), VALID_FROM, null);

        ProfileAssignment profileAssignment = ProfileAssignmentPersistenceMapper.toDomain(entity);

        assertThat(profileAssignment.validity().validUntil()).isEmpty();
    }

    @Test
    void a_present_valid_until_becomes_a_validity_with_end() {
        String validUntil = "2026-10-12T00:00:00Z";
        ProfileAssignmentEntity entity = new ProfileAssignmentEntity(ID, USER_ID, TENANT, APPLICATION_ID, PROFILE_ID,
                List.of(GENERATED_ID), VALID_FROM, validUntil);

        ProfileAssignment profileAssignment = ProfileAssignmentPersistenceMapper.toDomain(entity);

        assertThat(profileAssignment.validity().validUntil()).contains(Instant.parse(validUntil));
    }
}
