package co.edu.uco.seguridad.pdp.applications.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ListApplicationsRequest;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationCredentialHash;
import co.edu.uco.seguridad.pdp.applications.domain.ApplicationCriteria;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ListApplicationsUseCaseImplTests {

    private static final TenantId UCO = new TenantId("universidad-uco");
    private static final Instant REGISTERED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void returns_the_page_that_the_port_produced() {
        Application app = application("gestion-academica");
        ListApplicationsUseCaseImpl useCase = new ListApplicationsUseCaseImpl(
                repositoryReturning(ResultPage.of(List.of(app), 1L, PageWindow.defaultWindow()), new ArrayList<>()));

        StepVerifier.create(useCase.execute(request(PageWindow.defaultWindow())))
                .assertNext(page -> {
                    assertThat(page.content()).hasSize(1);
                    assertThat(page.content().get(0).name()).isEqualTo(app.name());
                    assertThat(page.total()).isEqualTo(1L);
                })
                .verifyComplete();
    }

    @Test
    void reports_the_total_of_the_whole_filter_not_of_the_page() {
        PageWindow firstOfMany = PageWindow.ofPage(0, 2);
        ListApplicationsUseCaseImpl useCase = new ListApplicationsUseCaseImpl(
                repositoryReturning(
                        ResultPage.of(List.of(application("uno"), application("dos")), 37L, firstOfMany),
                        new ArrayList<>()));

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
        ListApplicationsUseCaseImpl useCase = new ListApplicationsUseCaseImpl(
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
        ApplicationCriteria criteria = ApplicationCriteria.of(UCO, Optional.of("portal"));
        ListApplicationsUseCaseImpl useCase = new ListApplicationsUseCaseImpl(
                repositoryReturning(ResultPage.of(List.of(), 0L, window), received));

        StepVerifier.create(useCase.execute(new ListApplicationsRequest(criteria, window))).expectNextCount(1)
                .verifyComplete();

        assertThat(received).hasSize(1);
        assertThat(received.get(0)[0]).isEqualTo(criteria);
        assertThat(received.get(0)[1]).isEqualTo(window);
    }

    private static ListApplicationsRequest request(PageWindow window) {
        return new ListApplicationsRequest(ApplicationCriteria.ofTenant(UCO), window);
    }

    private static Application application(String name) {
        return new Application(new ApplicationId(UUID.randomUUID()), UCO, new ApplicationName(name), "",
                new ApplicationBaseUrl("https://example.com"), new ApplicationCredentialHash("hash"), REGISTERED_AT);
    }

    private static ApplicationRepository repositoryReturning(ResultPage<Application> page, List<Object[]> received) {
        return new ApplicationRepository() {
            @Override
            public Mono<Boolean> existsByTenantAndName(TenantId tenantId, ApplicationName name) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Boolean> existsByTenantAndId(TenantId tenantId, ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<TenantId> findTenantIdById(ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ResultPage<Application>> findBy(ApplicationCriteria criteria, PageWindow window) {
                received.add(new Object[] {criteria, window});
                return Mono.just(page);
            }

            @Override
            public Mono<Application> save(Application application) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Void> deleteById(ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
