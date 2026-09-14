package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.RotateApplicationCredentialRequest;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.ApplicationRegistrationResponse;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RotateApplicationCredentialUseCase;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
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

/**
 * Mismo patrón que {@link AdministerApplicationRemovalUseCaseImplTests}: valida y delega en
 * {@code RotateApplicationCredentialUseCase}, que no cambia.
 */
class AdministerApplicationCredentialRotationUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final AdministrationRequest REQUEST =
            new AdministrationRequest(TENANT, APPLICATION, USER, "test-subject", Set.of());

    @Test
    void rotates_the_credential_when_the_principal_administers_the_application() {
        List<RotateApplicationCredentialRequest> received = new ArrayList<>();
        AdministerApplicationCredentialRotationUseCaseImpl useCase = new AdministerApplicationCredentialRotationUseCaseImpl(
                allows(), rotateCredentialCapturing(received));

        StepVerifier.create(useCase.execute(REQUEST))
                .assertNext(response -> {
                    assertThat(response.credential()).isEqualTo("nuevo-secreto");
                    assertThat(response.application().id()).isEqualTo(APPLICATION);
                })
                .verifyComplete();

        assertThat(received).containsExactly(new RotateApplicationCredentialRequest(TENANT, APPLICATION));
    }

    @Test
    void never_rotates_the_credential_when_the_principal_does_not_administer_the_application() {
        RotateApplicationCredentialUseCase rotateCredential =
                request -> { throw new AssertionError("must not reach rotation"); };
        AdministerApplicationCredentialRotationUseCaseImpl useCase = new AdministerApplicationCredentialRotationUseCaseImpl(
                denies(), rotateCredential);

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

    private static RotateApplicationCredentialUseCase rotateCredentialCapturing(
            List<RotateApplicationCredentialRequest> received) {
        return request -> {
            received.add(request);
            return Mono.just(new ApplicationRegistrationResponse(
                    new RegisteredApplicationResponse(APPLICATION, TENANT, new ApplicationName("gestion-academica"),
                            "Sistema académico", new ApplicationBaseUrl("https://example.com"),
                            Instant.parse("2026-09-13T00:00:00Z")),
                    "nuevo-secreto"));
        };
    }
}
