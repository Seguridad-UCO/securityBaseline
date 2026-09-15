package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.ListProfilesUseCase;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.request.raw.ListProfilesRawRequest;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.shared.security.TestJwtSupport;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Lee el principal, delega en ListProfilesUseCase y proyecta ResultPage a PageResponse. */
class ListProfilesInteractorImplTests {

    private static final String TENANT = "universidad-uco";
    private static final String SUBJECT = "test-subject";

    @Test
    void lists_profiles_for_the_tenant_of_the_principal() {
        ProfileResponse response = new ProfileResponse(new ProfileId(UUID.randomUUID()), new ProfileName("Coordinadores"),
                RoleScope.ofTenant(new co.edu.uco.seguridad.pdp.commons.model.TenantId(TENANT)), Set.of(),
                Instant.parse("2026-09-15T00:00:00Z"));
        ResultPage<ProfileResponse> page = ResultPage.of(List.of(response), 1L, new PageWindow(0, 20));
        ListProfilesInteractorImpl interactor = new ListProfilesInteractorImpl(useCaseReturning(page));

        StepVerifier.create(interactor.execute(new ListProfilesRawRequest(null, null, null, null))
                        .contextWrite(TestJwtSupport.withPrincipal(TENANT, SUBJECT)))
                .assertNext(result -> {
                    assertThat(result.content()).hasSize(1);
                    assertThat(result.content().getFirst().id()).isEqualTo(response.id().value().toString());
                    assertThat(result.total()).isEqualTo(1L);
                })
                .verifyComplete();
    }

    private static ListProfilesUseCase useCaseReturning(ResultPage<ProfileResponse> page) {
        return input -> Mono.just(page);
    }
}
