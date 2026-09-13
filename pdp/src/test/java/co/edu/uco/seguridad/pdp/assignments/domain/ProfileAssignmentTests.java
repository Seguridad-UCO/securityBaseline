package co.edu.uco.seguridad.pdp.assignments.domain;

import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Espejo de AssignmentTests, con el conjunto de Assignment generadas que hace posible la cascada. */
class ProfileAssignmentTests {

    private static final ProfileAssignmentId ID = new ProfileAssignmentId(UUID.randomUUID());
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final ProfileId PROFILE = new ProfileId(UUID.randomUUID());
    private static final Set<AssignmentId> GENERATED = Set.of(new AssignmentId(UUID.randomUUID()));
    private static final Instant NOW = Instant.parse("2026-09-12T00:00:00Z");

    @Test
    void grant_starts_with_no_end_and_keeps_the_generated_assignment_ids() {
        ProfileAssignment profileAssignment = ProfileAssignment.grant(ID, USER, TENANT, APPLICATION, PROFILE, GENERATED, NOW);

        assertThat(profileAssignment.id()).isEqualTo(ID);
        assertThat(profileAssignment.userId()).isEqualTo(USER);
        assertThat(profileAssignment.tenantId()).isEqualTo(TENANT);
        assertThat(profileAssignment.applicationId()).isEqualTo(APPLICATION);
        assertThat(profileAssignment.profileId()).isEqualTo(PROFILE);
        assertThat(profileAssignment.generatedAssignmentIds()).isEqualTo(GENERATED);
        assertThat(profileAssignment.validity().validFrom()).isEqualTo(NOW);
        assertThat(profileAssignment.validity().validUntil()).isEmpty();
    }

    @Test
    void revoke_fixes_the_end_without_mutating_the_original() {
        ProfileAssignment profileAssignment = ProfileAssignment.grant(ID, USER, TENANT, APPLICATION, PROFILE, GENERATED, NOW);
        Instant revokedAt = NOW.plusSeconds(3600);

        ProfileAssignment revoked = profileAssignment.revoke(revokedAt);

        assertThat(revoked.validity().validUntil()).contains(revokedAt);
        assertThat(profileAssignment.validity().validUntil()).isEmpty();
    }

    @Test
    void is_active_delegates_to_the_validity() {
        ProfileAssignment profileAssignment = ProfileAssignment.grant(ID, USER, TENANT, APPLICATION, PROFILE, GENERATED, NOW);
        ProfileAssignment revoked = profileAssignment.revoke(NOW.plusSeconds(60));

        assertThat(profileAssignment.isActive(NOW.plusSeconds(30))).isTrue();
        assertThat(revoked.isActive(NOW.plusSeconds(120))).isFalse();
    }
}
