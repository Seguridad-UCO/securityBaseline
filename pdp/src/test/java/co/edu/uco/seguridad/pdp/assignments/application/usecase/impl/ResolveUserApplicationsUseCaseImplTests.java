package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveUserApplicationsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.domain.Assignment;
import co.edu.uco.seguridad.pdp.assignments.domain.AssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignment;
import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ResolveUserApplicationsUseCase;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.ProfileRolesLookupValidator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ResolveUserApplicationsUseCaseImplTests {

    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");
    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final RoleId DIRECT_ROLE = new RoleId(UUID.randomUUID());
    private static final RoleId PROFILE_ROLE = new RoleId(UUID.randomUUID());
    private static final ProfileId PROFILE = new ProfileId(UUID.randomUUID());

    @Test
    void returns_active_direct_roles() {
        resolve(List.of(assignment(DIRECT_ROLE)), List.of(), Set.of())
                .assertNext(result -> assertThat(result.rolesByApplication())
                        .containsEntry(APPLICATION, Set.of(DIRECT_ROLE)))
                .verifyComplete();
    }

    @Test
    void returns_roles_inherited_from_an_active_profile() {
        resolve(List.of(), List.of(profileAssignment()), Set.of(PROFILE_ROLE))
                .assertNext(result -> assertThat(result.rolesByApplication())
                        .containsEntry(APPLICATION, Set.of(PROFILE_ROLE)))
                .verifyComplete();
    }

    @Test
    void combines_sources_without_duplicate_roles() {
        resolve(List.of(assignment(DIRECT_ROLE)), List.of(profileAssignment()), Set.of(DIRECT_ROLE, PROFILE_ROLE))
                .assertNext(result -> assertThat(result.rolesByApplication().get(APPLICATION))
                        .containsExactlyInAnyOrder(DIRECT_ROLE, PROFILE_ROLE))
                .verifyComplete();
    }

    @Test
    void excludes_expired_assignments_before_resolving_roles() {
        resolve(List.of(), List.of(), Set.of())
                .assertNext(result -> assertThat(result.rolesByApplication()).isEmpty())
                .verifyComplete();
    }

    private static StepVerifier.FirstStep<co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.UserApplicationRolesResponse> resolve(
            List<Assignment> assignments, List<ProfileAssignment> profiles, Set<RoleId> profileRoles) {
        ResolveUserApplicationsUseCase useCase = new ResolveUserApplicationsUseCaseImpl(
                assignmentRepository(assignments), profileAssignmentRepository(profiles),
                query -> Mono.just(profileRoles), fixedTime());
        return StepVerifier.create(useCase.execute(new ResolveUserApplicationsRequest(USER, TENANT)));
    }

    private static Assignment assignment(RoleId roleId) {
        return Assignment.assign(new AssignmentId(UUID.randomUUID()), USER, TENANT, APPLICATION, roleId, NOW);
    }

    private static ProfileAssignment profileAssignment() {
        return ProfileAssignment.grant(new ProfileAssignmentId(UUID.randomUUID()), USER, TENANT, APPLICATION,
                PROFILE, Set.of(), NOW);
    }

    private static TimeProvider fixedTime() {
        return () -> NOW;
    }

    private static AssignmentRepository assignmentRepository(List<Assignment> active) {
        return new AssignmentRepository() {
            @Override public Mono<Boolean> existsActiveByUserApplicationRole(UserId userId, ApplicationId applicationId, RoleId roleId, Instant now) { return Mono.just(false); }
            @Override public Mono<Assignment> findByIdForTenant(AssignmentId assignmentId, TenantId tenantId) { return Mono.empty(); }
            @Override public Mono<ResultPage<Assignment>> findBy(AssignmentCriteria criteria, PageWindow window) { return Mono.just(ResultPage.of(List.of(), 0, window)); }
            @Override public Mono<Set<RoleId>> findActiveRoleIdsFor(UserId userId, ApplicationId applicationId, Instant now) { return Mono.just(Set.of()); }
            @Override public Flux<Assignment> findActiveByUserAndTenant(UserId userId, TenantId tenantId, Instant now) { return Flux.fromIterable(active); }
            @Override public Mono<Assignment> save(Assignment assignment) { return Mono.just(assignment); }
        };
    }

    private static ProfileAssignmentRepository profileAssignmentRepository(List<ProfileAssignment> active) {
        return new ProfileAssignmentRepository() {
            @Override public Mono<Boolean> existsActiveByUserApplicationProfile(UserId userId, ApplicationId applicationId, ProfileId profileId, Instant now) { return Mono.just(false); }
            @Override public Mono<ProfileAssignment> findByIdForTenant(ProfileAssignmentId id, TenantId tenantId) { return Mono.empty(); }
            @Override public Mono<ResultPage<ProfileAssignment>> findBy(ProfileAssignmentCriteria criteria, PageWindow window) { return Mono.just(ResultPage.of(List.of(), 0, window)); }
            @Override public Mono<ProfileAssignment> save(ProfileAssignment assignment) { return Mono.just(assignment); }
            @Override public Flux<ProfileAssignment> findActiveByUserAndTenant(UserId userId, TenantId tenantId, Instant now) { return Flux.fromIterable(active); }
        };
    }
}
