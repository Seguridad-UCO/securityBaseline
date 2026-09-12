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

class AssignmentTests {

    private static final AssignmentId ID = new AssignmentId(UUID.randomUUID());
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final RoleId ROLE = new RoleId(UUID.randomUUID());
    private static final Instant NOW = Instant.parse("2026-09-12T00:00:00Z");

    @Test
    void assign_starts_with_no_end() {
        Assignment assignment = Assignment.assign(ID, USER, TENANT, APPLICATION, ROLE, NOW);

        assertThat(assignment.id()).isEqualTo(ID);
        assertThat(assignment.userId()).isEqualTo(USER);
        assertThat(assignment.tenantId()).isEqualTo(TENANT);
        assertThat(assignment.applicationId()).isEqualTo(APPLICATION);
        assertThat(assignment.roleId()).isEqualTo(ROLE);
        assertThat(assignment.validity().validFrom()).isEqualTo(NOW);
        assertThat(assignment.validity().validUntil()).isEmpty();
    }

    @Test
    void revoke_fixes_the_end_without_mutating_the_original() {
        Assignment assignment = Assignment.assign(ID, USER, TENANT, APPLICATION, ROLE, NOW);
        Instant revokedAt = NOW.plusSeconds(3600);

        Assignment revoked = assignment.revoke(revokedAt);

        assertThat(revoked.validity().validUntil()).contains(revokedAt);
        assertThat(assignment.validity().validUntil()).isEmpty();
    }

    @Test
    void is_active_delegates_to_the_validity() {
        Assignment assignment = Assignment.assign(ID, USER, TENANT, APPLICATION, ROLE, NOW);
        Assignment revoked = assignment.revoke(NOW.plusSeconds(60));

        assertThat(assignment.isActive(NOW.plusSeconds(30))).isTrue();
        assertThat(revoked.isActive(NOW.plusSeconds(120))).isFalse();
    }
}
