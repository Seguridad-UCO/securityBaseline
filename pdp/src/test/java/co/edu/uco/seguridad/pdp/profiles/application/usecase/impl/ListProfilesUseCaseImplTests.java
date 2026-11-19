package co.edu.uco.seguridad.pdp.profiles.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ListProfilesRequest;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.domain.Profile;
import co.edu.uco.seguridad.pdp.profiles.domain.ProfileCriteria;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Espejo de ListRolesUseCaseImplTests: la página que produce el puerto (tenant + globales) viaja sin cambios.
 */
class ListProfilesUseCaseImplTests {

    private static final TenantId UCO = new TenantId("universidad-uco");
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-12T00:00:00Z");

    @Test
    void returns_the_page_that_the_port_produced() {
        Profile profile = Profile.define(new ProfileId(UUID.randomUUID()), new ProfileName("Coordinador académico"),
                RoleScope.ofTenant(UCO), REGISTERED_AT);
        ListProfilesUseCaseImpl useCase = new ListProfilesUseCaseImpl(
                repositoryReturning(ResultPage.of(List.of(profile), 1L, PageWindow.defaultWindow())));

        StepVerifier.create(useCase.execute(request(PageWindow.defaultWindow())))
                .assertNext(page -> {
                    assertThat(page.content()).hasSize(1);
                    assertThat(page.total()).isEqualTo(1L);
                })
                .verifyComplete();
    }

    @Test
    void returns_an_empty_page_instead_of_failing_when_nothing_matches() {
        ListProfilesUseCaseImpl useCase = new ListProfilesUseCaseImpl(
                repositoryReturning(ResultPage.of(List.of(), 0L, PageWindow.defaultWindow())));

        StepVerifier.create(useCase.execute(request(PageWindow.defaultWindow())))
                .assertNext(page -> {
                    assertThat(page.content()).isEmpty();
                    assertThat(page.total()).isZero();
                })
                .verifyComplete();
    }

    private static ListProfilesRequest request(PageWindow window) {
        return new ListProfilesRequest(ProfileCriteria.ofTenant(UCO), window);
    }

    private static ProfileRepository repositoryReturning(ResultPage<Profile> page) {
        return new ProfileRepository() {
            @Override
            public Mono<Boolean> existsByNameInScope(ProfileName name, RoleScope scope) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Profile> findByIdForTenant(ProfileId profileId, TenantId tenantId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ResultPage<Profile>> findBy(ProfileCriteria criteria, PageWindow window) {
                return Mono.just(page);
            }

            @Override
            public Mono<Profile> save(Profile profile) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
