package co.edu.uco.seguridad.pdp.roles.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.ListRolesRequest;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.domain.Role;
import co.edu.uco.seguridad.pdp.roles.domain.RoleCriteria;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ListRolesUseCaseImplTests {

    private static final TenantId UCO = new TenantId("universidad-uco");
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-11T00:00:00Z");

    @Test
    void returns_the_page_that_the_port_produced() {
        Role role = role("Docente");
        ListRolesUseCaseImpl useCase = new ListRolesUseCaseImpl(
                repositoryReturning(ResultPage.of(List.of(role), 1L, PageWindow.defaultWindow()), new ArrayList<>()));

        StepVerifier.create(useCase.execute(request(PageWindow.defaultWindow())))
                .assertNext(page -> {
                    assertThat(page.content()).hasSize(1);
                    assertThat(page.content().get(0).name()).isEqualTo(role.name());
                    assertThat(page.total()).isEqualTo(1L);
                })
                .verifyComplete();
    }

    @Test
    void reports_the_total_of_the_whole_filter_not_of_the_page() {
        PageWindow firstOfMany = PageWindow.ofPage(0, 2);
        ListRolesUseCaseImpl useCase = new ListRolesUseCaseImpl(repositoryReturning(
                ResultPage.of(List.of(role("uno"), role("dos")), 37L, firstOfMany), new ArrayList<>()));

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
        ListRolesUseCaseImpl useCase = new ListRolesUseCaseImpl(
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
        RoleCriteria criteria = RoleCriteria.ofTenant(UCO);
        ListRolesUseCaseImpl useCase = new ListRolesUseCaseImpl(
                repositoryReturning(ResultPage.of(List.of(), 0L, window), received));

        StepVerifier.create(useCase.execute(new ListRolesRequest(criteria, window))).expectNextCount(1).verifyComplete();

        assertThat(received).hasSize(1);
        assertThat(received.get(0)[0]).isEqualTo(criteria);
        assertThat(received.get(0)[1]).isEqualTo(window);
    }

    private static ListRolesRequest request(PageWindow window) {
        return new ListRolesRequest(RoleCriteria.ofTenant(UCO), window);
    }

    private static Role role(String name) {
        return Role.define(new RoleId(UUID.randomUUID()), new RoleName(name), RoleScope.ofTenant(UCO), REGISTERED_AT);
    }

    private static RoleRepository repositoryReturning(ResultPage<Role> page, List<Object[]> received) {
        return new RoleRepository() {
            @Override
            public Mono<Boolean> existsByNameInScope(RoleName name, RoleScope scope) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Role> findByIdForTenant(RoleId roleId, TenantId tenantId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Role> findById(RoleId roleId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ResultPage<Role>> findBy(RoleCriteria criteria, PageWindow window) {
                received.add(new Object[] {criteria, window});
                return Mono.just(page);
            }

            @Override
            public Mono<Role> save(Role role) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
