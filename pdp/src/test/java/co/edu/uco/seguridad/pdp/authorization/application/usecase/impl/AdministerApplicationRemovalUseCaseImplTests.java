package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.usecase.RemoveApplicationUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No decide nada de negocio propio: valida con el mecanismo de HU-009 y delega en
 * {@code RemoveApplicationUseCase}, que no cambia (PLAN-HU-015.md §0).
 */
class AdministerApplicationRemovalUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final AdministrationRequest REQUEST =
            new AdministrationRequest(TENANT, APPLICATION, USER, "test-subject", Set.of());

    @Test
    void removes_the_application_when_the_principal_administers_it() {
        List<ApplicationId> removed = new ArrayList<>();
        AdministerApplicationRemovalUseCaseImpl useCase = new AdministerApplicationRemovalUseCaseImpl(
                allows(), removeApplicationCapturing(removed));

        StepVerifier.create(useCase.execute(REQUEST)).verifyComplete();

        assertThat(removed).containsExactly(APPLICATION);
    }

    @Test
    void never_removes_the_application_when_the_principal_does_not_administer_it() {
        RemoveApplicationUseCase removeApplication = id -> { throw new AssertionError("must not reach removal"); };
        AdministerApplicationRemovalUseCaseImpl useCase = new AdministerApplicationRemovalUseCaseImpl(denies(), removeApplication);

        StepVerifier.create(useCase.execute(REQUEST))
                .expectError(NotAuthorizedToAdministerException.class)
                .verify();
    }

    private static PrincipalMustBeApplicationAdministratorValidator allows() {
        return request -> Mono.empty();
    }

    private static PrincipalMustBeApplicationAdministratorValidator denies() {
        return request -> Mono.error(new NotAuthorizedToAdministerException(TENANT, APPLICATION));
    }

    private static RemoveApplicationUseCase removeApplicationCapturing(List<ApplicationId> received) {
        return id -> {
            received.add(id);
            return Mono.empty();
        };
    }
}
