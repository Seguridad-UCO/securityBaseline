package co.edu.uco.seguridad.pdp.assignments.domain;

import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AssignmentCriteriaTests {

    private static final RoleId ROLE = new RoleId(UUID.randomUUID());
    private static final RoleId OTHER_ROLE = new RoleId(UUID.randomUUID());
    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final TenantId OTHER_TENANT = new TenantId("otra-universidad");
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-12T00:00:00Z");

    @Test
    void matches_an_assignment_of_the_same_role_and_tenant() {
        AssignmentCriteria criteria = AssignmentCriteria.of(ROLE, TENANT);

        assertThat(criteria.matches(assignment(ROLE, TENANT))).isTrue();
    }

    @Test
    void does_not_match_the_same_role_in_another_tenant() {
        AssignmentCriteria criteria = AssignmentCriteria.of(ROLE, TENANT);

        assertThat(criteria.matches(assignment(ROLE, OTHER_TENANT))).isFalse();
    }

    @Test
    void does_not_match_another_role_in_the_same_tenant() {
        AssignmentCriteria criteria = AssignmentCriteria.of(ROLE, TENANT);

        assertThat(criteria.matches(assignment(OTHER_ROLE, TENANT))).isFalse();
    }

    private static Assignment assignment(RoleId roleId, TenantId tenantId) {
        return Assignment.assign(new AssignmentId(UUID.randomUUID()), new UserId(UUID.randomUUID()), tenantId,
                new ApplicationId(UUID.randomUUID()), roleId, REGISTERED_AT);
    }
}
