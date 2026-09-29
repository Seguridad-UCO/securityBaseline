package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListApplicationAdministratorsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListApplicationAdministratorsUseCase;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerApplicationAdministratorListRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * HU-020: gatea ListApplicationAdministratorsUseCase tras HU-009.
 */
class AdministerApplicationAdministratorListUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final UserId ADMIN_USER = new UserId(UUID.randomUUID());
    private static final AdministrationRequest ADMINISTRATION =
            new AdministrationRequest(TENANT, APPLICATION, ADMIN_USER, "test-subject", Set.of());
    private static final ListApplicationAdministratorsRequest LIST_REQUEST =
            new ListApplicationAdministratorsRequest(TENANT, APPLICATION);
    private static final List<AssignmentResponse> ADMINISTRATORS = List.of(new AssignmentResponse(
            new AssignmentId(UUID.randomUUID()), ADMIN_USER, TENANT, APPLICATION, new RoleId(UUID.randomUUID()),
            Instant.parse("2026-09-15T00:00:00Z"), Optional.empty()));

    @Test
    void lists_the_administrators_when_the_principal_administers_the_application() {
        AdministerApplicationAdministratorListUseCaseImpl useCase =
                new AdministerApplicationAdministratorListUseCaseImpl(allows(), listReturning(ADMINISTRATORS));

        StepVerifier.create(useCase.execute(new AdministerApplicationAdministratorListRequest(ADMINISTRATION, LIST_REQUEST)))
                .expectNext(ADMINISTRATORS)
                .verifyComplete();
    }

    @Test
    void never_lists_the_administrators_when_the_principal_does_not_administer_the_application() {
        ListApplicationAdministratorsUseCase listApplicationAdministrators = input -> {
            throw new AssertionError("must not reach ListApplicationAdministratorsUseCase");
        };
        AdministerApplicationAdministratorListUseCaseImpl useCase =
                new AdministerApplicationAdministratorListUseCaseImpl(denies(), listApplicationAdministrators);

        StepVerifier.create(useCase.execute(new AdministerApplicationAdministratorListRequest(ADMINISTRATION, LIST_REQUEST)))
                .expectError(NotAuthorizedToAdministerException.class)
                .verify();
    }

    private static PrincipalMustBeApplicationAdministratorValidator allows() {
        return request -> Mono.empty();
    }

    private static PrincipalMustBeApplicationAdministratorValidator denies() {
        return request -> Mono.error(new NotAuthorizedToAdministerException(TENANT, APPLICATION));
    }

    private static ListApplicationAdministratorsUseCase listReturning(List<AssignmentResponse> administrators) {
        return input -> Mono.just(administrators);
    }
}
