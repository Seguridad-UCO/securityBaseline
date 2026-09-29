package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ReadApplicationSecuritySummaryRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ApplicationAssignmentCountsResponse;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class ReadApplicationSecuritySummaryUseCaseImplTests {
    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final AdministrationRequest ADMINISTRATION =
            new AdministrationRequest(TENANT, APPLICATION, new UserId(UUID.randomUUID()), "subject", Set.of());

    @Test
    void gates_before_delegating_counts_and_returns_only_owner_counts() {
        var useCase = new ReadApplicationSecuritySummaryUseCaseImpl(
                allowed(), app -> Mono.just(application()), app -> Mono.just(11L), criteria -> Mono.just(7L),
                criteria -> Mono.just(3L), request -> Mono.just(new ApplicationAssignmentCountsResponse(17, 5, 2)));

        StepVerifier.create(useCase.execute(new ReadApplicationSecuritySummaryRequest(ADMINISTRATION)))
                .assertNext(summary -> {
                    assertThat(summary.application().id()).isEqualTo(APPLICATION.value().toString());
                    assertThat(summary.counts().resources()).isEqualTo(11);
                    assertThat(summary.counts().roles()).isEqualTo(7);
                    assertThat(summary.counts().profiles()).isEqualTo(3);
                    assertThat(summary.counts().administrators()).isEqualTo(2);
                    assertThat(summary.counts().roleAssignments()).isEqualTo(17);
                    assertThat(summary.counts().profileAssignments()).isEqualTo(5);
                })
                .verifyComplete();
    }

    @Test
    void does_not_query_any_owner_module_when_administration_is_denied() {
        AtomicBoolean queried = new AtomicBoolean();
        var forbidden = new ReadApplicationSecuritySummaryUseCaseImpl(denied(), app -> called(queried),
                app -> called(queried), criteria -> called(queried), criteria -> called(queried), request -> called(queried));

        StepVerifier.create(forbidden.execute(new ReadApplicationSecuritySummaryRequest(ADMINISTRATION)))
                .expectError(NotAuthorizedToAdministerException.class)
                .verify();

        assertThat(queried).isFalse();
    }

    private static PrincipalMustBeApplicationAdministratorValidator allowed() {
        return request -> Mono.empty();
    }

    private static PrincipalMustBeApplicationAdministratorValidator denied() {
        return request -> Mono.error(new NotAuthorizedToAdministerException(TENANT, APPLICATION));
    }

    private static RegisteredApplicationResponse application() {
        return new RegisteredApplicationResponse(APPLICATION, TENANT, new ApplicationName("Notas"), "",
                new ApplicationBaseUrl("https://notas.example.edu"), Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static <T> Mono<T> called(AtomicBoolean queried) {
        queried.set(true);
        return Mono.error(new AssertionError("must not query after the administrative gate"));
    }
}
