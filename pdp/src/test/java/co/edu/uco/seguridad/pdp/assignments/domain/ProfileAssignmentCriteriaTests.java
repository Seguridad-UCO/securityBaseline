package co.edu.uco.seguridad.pdp.assignments.domain;

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

class ProfileAssignmentCriteriaTests {

    private static final ProfileId PROFILE = ProfileId.of(UUID.randomUUID().toString());
    private static final ProfileId OTHER_PROFILE = ProfileId.of(UUID.randomUUID().toString());
    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final TenantId OTHER_TENANT = new TenantId("otra-universidad");
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-12T00:00:00Z");

    @Test
    void matches_a_profile_assignment_of_the_same_profile_and_tenant() {
        ProfileAssignmentCriteria criteria = ProfileAssignmentCriteria.of(PROFILE, TENANT);

        assertThat(criteria.matches(profileAssignment(PROFILE, TENANT))).isTrue();
    }

    @Test
    void does_not_match_the_same_profile_in_another_tenant() {
        ProfileAssignmentCriteria criteria = ProfileAssignmentCriteria.of(PROFILE, TENANT);

        assertThat(criteria.matches(profileAssignment(PROFILE, OTHER_TENANT))).isFalse();
    }

    @Test
    void does_not_match_another_profile_in_the_same_tenant() {
        ProfileAssignmentCriteria criteria = ProfileAssignmentCriteria.of(PROFILE, TENANT);

        assertThat(criteria.matches(profileAssignment(OTHER_PROFILE, TENANT))).isFalse();
    }

    private static ProfileAssignment profileAssignment(ProfileId profileId, TenantId tenantId) {
        return ProfileAssignment.grant(new ProfileAssignmentId(UUID.randomUUID()), new UserId(UUID.randomUUID()),
                tenantId, new ApplicationId(UUID.randomUUID()), profileId, Set.of(), REGISTERED_AT);
    }
}
