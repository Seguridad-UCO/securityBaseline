package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.applications.domain.exception.ApplicationNotFoundException;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignApplicationAdministratorRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignApplicationAdministratorUseCase;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.AssignApplicationAdministratorRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import co.edu.uco.seguridad.shared.web.exception.MissingRequestFieldException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El tenant sale del catálogo (ApplicationOwnerLookupValidator), nunca del cuerpo — mismo criterio
 * que el canal interno de HU-003.
 */
class AssignApplicationAdministratorInteractorImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final UserId USER = new UserId(UUID.randomUUID());

    @Test
    void resolves_the_tenant_from_the_catalog_and_delegates_to_the_use_case() {
        List<AssignApplicationAdministratorRequest> received = new ArrayList<>();
        AssignApplicationAdministratorInteractorImpl interactor = new AssignApplicationAdministratorInteractorImpl(
                ownerLookupResolving(TENANT), useCaseCapturing(received));

        var response = interactor.execute(
                new AssignApplicationAdministratorRawRequest(APPLICATION.value().toString(), USER.value().toString()));

        StepVerifier.create(response).assertNext(webResponse -> assertThat(webResponse.userId())
                .isEqualTo(USER.value().toString())).verifyComplete();

        assertThat(received).hasSize(1);
        assertThat(received.getFirst().tenantId()).isEqualTo(TENANT);
        assertThat(received.getFirst().applicationId()).isEqualTo(APPLICATION);
        assertThat(received.getFirst().userId()).isEqualTo(USER);
    }

    @Test
    void never_reaches_the_use_case_when_the_application_does_not_exist() {
        AssignApplicationAdministratorUseCase useCase = request -> { throw new AssertionError("must not reach the use case"); };
        AssignApplicationAdministratorInteractorImpl interactor = new AssignApplicationAdministratorInteractorImpl(
                ownerLookupFailing(), useCase);

        StepVerifier.create(interactor.execute(
                        new AssignApplicationAdministratorRawRequest(APPLICATION.value().toString(), USER.value().toString())))
                .expectError(ApplicationNotFoundException.class)
                .verify();
    }

    @Test
    void rejects_a_user_id_that_is_not_a_valid_identifier_after_resolving_the_tenant() {
        AssignApplicationAdministratorUseCase useCase = request -> { throw new AssertionError("must not reach the use case"); };
        AssignApplicationAdministratorInteractorImpl interactor = new AssignApplicationAdministratorInteractorImpl(
                ownerLookupResolving(TENANT), useCase);

        StepVerifier.create(interactor.execute(
                        new AssignApplicationAdministratorRawRequest(APPLICATION.value().toString(), "not-a-uuid")))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(MalformedRequestFieldException.class)
                        .extracting(e -> ((MalformedRequestFieldException) e).field())
                        .isEqualTo("userId"))
                .verify();
    }

    @Test
    void requires_the_application_id_field() {
        ApplicationOwnerLookupValidator ownerLookup = id -> { throw new AssertionError("must not look up the owner"); };
        AssignApplicationAdministratorUseCase useCase = request -> { throw new AssertionError("must not reach the use case"); };
        AssignApplicationAdministratorInteractorImpl interactor = new AssignApplicationAdministratorInteractorImpl(
                ownerLookup, useCase);

        StepVerifier.create(interactor.execute(new AssignApplicationAdministratorRawRequest(null, USER.value().toString())))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(MissingRequestFieldException.class)
                        .extracting(e -> ((MissingRequestFieldException) e).field())
                        .isEqualTo("applicationId"))
                .verify();
    }

    private static ApplicationOwnerLookupValidator ownerLookupResolving(TenantId tenantId) {
        return applicationId -> Mono.just(tenantId);
    }

    private static ApplicationOwnerLookupValidator ownerLookupFailing() {
        return applicationId -> Mono.error(new ApplicationNotFoundException(applicationId));
    }

    private static AssignApplicationAdministratorUseCase useCaseCapturing(List<AssignApplicationAdministratorRequest> received) {
        return request -> {
            received.add(request);
            return Mono.just(new AssignmentResponse(new AssignmentId(UUID.randomUUID()), request.userId(),
                    request.tenantId(), request.applicationId(), new RoleId(UUID.randomUUID()),
                    Instant.parse("2026-09-13T00:00:00Z"), Optional.empty()));
        };
    }
}
