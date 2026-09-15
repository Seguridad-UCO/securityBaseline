package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListProfileAssignmentsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignment;
import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ListProfileAssignmentsUseCaseImplTests {

    private static final TenantId UCO = new TenantId("universidad-uco");
    private static final ProfileId PROFILE = ProfileId.of(UUID.randomUUID().toString());
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-12T00:00:00Z");

    @Test
    void returns_the_page_that_the_port_produced() {
        ProfileAssignment profileAssignment = profileAssignment();
        ListProfileAssignmentsUseCaseImpl useCase = new ListProfileAssignmentsUseCaseImpl(
                repositoryReturning(ResultPage.of(List.of(profileAssignment), 1L, PageWindow.defaultWindow()),
                        new ArrayList<>()));

        StepVerifier.create(useCase.execute(request(PageWindow.defaultWindow())))
                .assertNext(page -> {
                    assertThat(page.content()).hasSize(1);
                    assertThat(page.content().get(0).userId()).isEqualTo(profileAssignment.userId());
                    assertThat(page.total()).isEqualTo(1L);
                })
                .verifyComplete();
    }

    @Test
    void reports_the_total_of_the_whole_filter_not_of_the_page() {
        PageWindow firstOfMany = PageWindow.ofPage(0, 2);
        ListProfileAssignmentsUseCaseImpl useCase = new ListProfileAssignmentsUseCaseImpl(repositoryReturning(
                ResultPage.of(List.of(profileAssignment(), profileAssignment()), 37L, firstOfMany), new ArrayList<>()));

        StepVerifier.create(useCase.execute(request(firstOfMany)))
                .assertNext(page -> {
                    assertThat(page.content()).hasSize(2);
                    assertThat(page.total()).isEqualTo(37L);
                    assertThat(page.window()).isEqualTo(firstOfMany);
                })
                .verifyComplete();
    }

    @Test
    void hands_the_criteria_and_the_window_to_the_port_untouched() {
        List<Object[]> received = new ArrayList<>();
        PageWindow window = PageWindow.ofRange(10, 5);
        ProfileAssignmentCriteria criteria = ProfileAssignmentCriteria.of(PROFILE, UCO);
        ListProfileAssignmentsUseCaseImpl useCase = new ListProfileAssignmentsUseCaseImpl(
                repositoryReturning(ResultPage.of(List.of(), 0L, window), received));

        StepVerifier.create(useCase.execute(new ListProfileAssignmentsRequest(criteria, window)))
                .expectNextCount(1).verifyComplete();

        assertThat(received).hasSize(1);
        assertThat(received.get(0)[0]).isEqualTo(criteria);
        assertThat(received.get(0)[1]).isEqualTo(window);
    }

    private static ListProfileAssignmentsRequest request(PageWindow window) {
        return new ListProfileAssignmentsRequest(ProfileAssignmentCriteria.of(PROFILE, UCO), window);
    }

    private static ProfileAssignment profileAssignment() {
        return ProfileAssignment.grant(new ProfileAssignmentId(UUID.randomUUID()), new UserId(UUID.randomUUID()), UCO,
                new ApplicationId(UUID.randomUUID()), PROFILE, Set.of(), REGISTERED_AT);
    }

    private static ProfileAssignmentRepository repositoryReturning(ResultPage<ProfileAssignment> page,
            List<Object[]> received) {
        return new ProfileAssignmentRepository() {
            @Override
            public Mono<Boolean> existsActiveByUserApplicationProfile(UserId userId, ApplicationId applicationId,
                    ProfileId profileId, Instant now) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ProfileAssignment> findByIdForTenant(ProfileAssignmentId profileAssignmentId, TenantId tenantId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ResultPage<ProfileAssignment>> findBy(ProfileAssignmentCriteria criteria, PageWindow window) {
                received.add(new Object[] {criteria, window});
                return Mono.just(page);
            }

            @Override
            public Mono<ProfileAssignment> save(ProfileAssignment profileAssignment) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
