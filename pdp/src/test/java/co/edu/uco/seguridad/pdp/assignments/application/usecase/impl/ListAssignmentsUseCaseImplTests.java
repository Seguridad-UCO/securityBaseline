package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListAssignmentsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.domain.Assignment;
import co.edu.uco.seguridad.pdp.assignments.domain.AssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.*;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ListAssignmentsUseCaseImplTests {

    private static final TenantId UCO = new TenantId("universidad-uco");
    private static final RoleId ROLE = new RoleId(UUID.randomUUID());
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-12T00:00:00Z");

    @Test
    void returns_the_page_that_the_port_produced() {
        Assignment assignment = assignment();
        ListAssignmentsUseCaseImpl useCase = new ListAssignmentsUseCaseImpl(
                repositoryReturning(ResultPage.of(List.of(assignment), 1L, PageWindow.defaultWindow()), new ArrayList<>()));

        StepVerifier.create(useCase.execute(request(PageWindow.defaultWindow())))
                .assertNext(page -> {
                    assertThat(page.content()).hasSize(1);
                    assertThat(page.content().get(0).userId()).isEqualTo(assignment.userId());
                    assertThat(page.total()).isEqualTo(1L);
                })
                .verifyComplete();
    }

    @Test
    void reports_the_total_of_the_whole_filter_not_of_the_page() {
        PageWindow firstOfMany = PageWindow.ofPage(0, 2);
        ListAssignmentsUseCaseImpl useCase = new ListAssignmentsUseCaseImpl(repositoryReturning(
                ResultPage.of(List.of(assignment(), assignment()), 37L, firstOfMany), new ArrayList<>()));

        StepVerifier.create(useCase.execute(request(firstOfMany)))
                .assertNext(page -> {
                    assertThat(page.content()).hasSize(2);
                    assertThat(page.total()).isEqualTo(37L);
                    assertThat(page.window()).isEqualTo(firstOfMany);
                })
                .verifyComplete();
    }

    @Test
    void returns_an_empty_page_instead_of_failing_when_nothing_matches() {
        ListAssignmentsUseCaseImpl useCase = new ListAssignmentsUseCaseImpl(
                repositoryReturning(ResultPage.of(List.of(), 0L, PageWindow.defaultWindow()), new ArrayList<>()));

        StepVerifier.create(useCase.execute(request(PageWindow.defaultWindow())))
                .assertNext(page -> {
                    assertThat(page.content()).isEmpty();
                    assertThat(page.total()).isZero();
                })
                .verifyComplete();
    }

    @Test
    void hands_the_criteria_and_the_window_to_the_port_untouched() {
        List<Object[]> received = new ArrayList<>();
        PageWindow window = PageWindow.ofRange(10, 5);
        AssignmentCriteria criteria = AssignmentCriteria.of(ROLE, UCO);
        ListAssignmentsUseCaseImpl useCase = new ListAssignmentsUseCaseImpl(
                repositoryReturning(ResultPage.of(List.of(), 0L, window), received));

        StepVerifier.create(useCase.execute(new ListAssignmentsRequest(criteria, window))).expectNextCount(1).verifyComplete();

        assertThat(received).hasSize(1);
        assertThat(received.get(0)[0]).isEqualTo(criteria);
        assertThat(received.get(0)[1]).isEqualTo(window);
    }

    private static ListAssignmentsRequest request(PageWindow window) {
        return new ListAssignmentsRequest(AssignmentCriteria.of(ROLE, UCO), window);
    }

    private static Assignment assignment() {
        return Assignment.assign(new AssignmentId(UUID.randomUUID()), new UserId(UUID.randomUUID()), UCO,
                new ApplicationId(UUID.randomUUID()), ROLE, REGISTERED_AT);
    }

    private static AssignmentRepository repositoryReturning(ResultPage<Assignment> page, List<Object[]> received) {
        return new AssignmentRepository() {
            @Override
            public Mono<Boolean> existsActiveByUserApplicationRole(UserId userId, ApplicationId applicationId, RoleId roleId,
                                                                   Instant now) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Assignment> findByIdForTenant(AssignmentId assignmentId, TenantId tenantId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ResultPage<Assignment>> findBy(AssignmentCriteria criteria, PageWindow window) {
                received.add(new Object[]{criteria, window});
                return Mono.just(page);
            }

            @Override
            public Mono<Set<RoleId>> findActiveRoleIdsFor(UserId userId, ApplicationId applicationId, Instant now) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Assignment> save(Assignment assignment) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
